/**
 * OData Service Handler
 * Handles all OData-related operations including metadata parsing and data fetching
 */

class ODataService {
    constructor(serviceUrl, authService = null) {
        this.serviceUrl = serviceUrl.endsWith('/') ? serviceUrl.slice(0, -1) : serviceUrl;
        this.metadata = null;
        this.entityTypes = new Map();
        this.complexTypes = new Map();
        this.entitySets = new Map();
        this.authService = authService;
    }

    /**
     * Set authentication service
     */
    setAuthService(authService) {
        this.authService = authService;
    }

    /**
     * Get request headers with authentication
     */
    getHeaders(contentType = 'application/json') {
        const headers = {
            'Accept': 'application/json',
            'Content-Type': contentType
        };

        if (this.authService) {
            const authHeader = this.authService.getAuthHeader();
            if (authHeader) {
                headers['Authorization'] = authHeader;
            }
        }

        return headers;
    }

    /**
     * Fetch and parse the $metadata endpoint
     */
    async fetchMetadata() {
        try {
            const response = await fetch(`${this.serviceUrl}/$metadata`);
            if (!response.ok) {
                throw new Error(`Failed to fetch metadata: ${response.statusText}`);
            }

            const xmlText = await response.text();
            const parser = new DOMParser();
            const xmlDoc = parser.parseFromString(xmlText, 'text/xml');

            // Check for parsing errors
            const parserError = xmlDoc.querySelector('parsererror');
            if (parserError) {
                throw new Error('Failed to parse metadata XML');
            }

            this.metadata = xmlDoc;
            this.parseMetadata(xmlDoc);

            return {
                entitySets: Array.from(this.entitySets.values()),
                entityTypes: Array.from(this.entityTypes.values())
            };
        } catch (error) {
            console.error('Error fetching metadata:', error);
            throw error;
        }
    }

    /**
     * Parse the metadata XML document
     */
    parseMetadata(xmlDoc) {
        // Parse Complex Types first (so they're available for entity types)
        const complexTypeElements = xmlDoc.querySelectorAll('ComplexType');
        complexTypeElements.forEach(complexType => {
            const name = complexType.getAttribute('Name');
            const namespace = complexType.parentElement.getAttribute('Namespace');
            const fullName = `${namespace}.${name}`;

            const properties = [];

            // Parse Properties
            complexType.querySelectorAll('Property').forEach(prop => {
                properties.push({
                    name: prop.getAttribute('Name'),
                    type: prop.getAttribute('Type'),
                    nullable: prop.getAttribute('Nullable') !== 'false',
                    maxLength: prop.getAttribute('MaxLength'),
                    precision: prop.getAttribute('Precision'),
                    scale: prop.getAttribute('Scale')
                });
            });

            this.complexTypes.set(fullName, {
                name,
                fullName,
                namespace,
                properties,
                isComplexType: true
            });
        });

        // Parse EntityTypes
        const entityTypeElements = xmlDoc.querySelectorAll('EntityType');
        entityTypeElements.forEach(entityType => {
            const name = entityType.getAttribute('Name');
            const namespace = entityType.parentElement.getAttribute('Namespace');
            const fullName = `${namespace}.${name}`;

            const properties = [];
            const navigationProperties = [];

            // Parse Properties
            entityType.querySelectorAll('Property').forEach(prop => {
                const propType = prop.getAttribute('Type');
                properties.push({
                    name: prop.getAttribute('Name'),
                    type: propType,
                    nullable: prop.getAttribute('Nullable') !== 'false',
                    maxLength: prop.getAttribute('MaxLength'),
                    precision: prop.getAttribute('Precision'),
                    scale: prop.getAttribute('Scale'),
                    isComplexType: this.complexTypes.has(propType)
                });
            });

            // Parse Keys
            const keys = [];
            entityType.querySelectorAll('Key PropertyRef').forEach(keyRef => {
                keys.push(keyRef.getAttribute('Name'));
            });

            // Parse Navigation Properties
            entityType.querySelectorAll('NavigationProperty').forEach(navProp => {
                navigationProperties.push({
                    name: navProp.getAttribute('Name'),
                    type: navProp.getAttribute('Type'),
                    isCollection: navProp.getAttribute('Type')?.startsWith('Collection(') || false,
                    partner: navProp.getAttribute('Partner')
                });
            });

            this.entityTypes.set(fullName, {
                name,
                fullName,
                namespace,
                properties,
                keys,
                navigationProperties
            });
        });

        // Parse EntitySets
        const entityContainers = xmlDoc.querySelectorAll('EntityContainer');
        entityContainers.forEach(container => {
            container.querySelectorAll('EntitySet').forEach(entitySet => {
                const name = entitySet.getAttribute('Name');
                const entityType = entitySet.getAttribute('EntityType');

                this.entitySets.set(name, {
                    name,
                    entityType,
                    entityTypeInfo: this.entityTypes.get(entityType)
                });
            });
        });
    }

    /**
     * Fetch entities from an entity set
     */
    async fetchEntitySet(entitySetName, options = {}) {
        const { top = 25, skip = 0, filter = null, orderby = null } = options;

        let url = `${this.serviceUrl}/${entitySetName}?$top=${top}&$skip=${skip}`;

        if (filter) {
            url += `&$filter=${encodeURIComponent(filter)}`;
        }

        if (orderby) {
            url += `&$orderby=${encodeURIComponent(orderby)}`;
        }

        // Add $count to get total count
        url += '&$count=true';

        try {
            const response = await fetch(url);
            if (!response.ok) {
                throw new Error(`Failed to fetch entity set: ${response.statusText}`);
            }

            const data = await response.json();

            return {
                value: data.value || [],
                count: data['@odata.count'] || data.value?.length || 0
            };
        } catch (error) {
            console.error('Error fetching entity set:', error);
            throw error;
        }
    }

    /**
     * Fetch a single entity by key
     */
    async fetchEntity(entitySetName, keyValue, keyProperty = null) {
        try {
            // Get entity set info
            const entitySet = this.entitySets.get(entitySetName);
            if (!entitySet) {
                throw new Error(`Entity set ${entitySetName} not found`);
            }

            const entityTypeInfo = entitySet.entityTypeInfo;

            // Build key predicate
            let keyPredicate;
            if (entityTypeInfo.keys.length === 1) {
                const key = entityTypeInfo.keys[0];
                const keyProp = entityTypeInfo.properties.find(p => p.name === key);
                const isString = keyProp?.type?.includes('String');
                keyPredicate = isString ? `'${keyValue}'` : keyValue;
            } else {
                // Composite key - assume keyValue is an object
                const keyParts = entityTypeInfo.keys.map(key => {
                    const keyProp = entityTypeInfo.properties.find(p => p.name === key);
                    const isString = keyProp?.type?.includes('String');
                    const value = typeof keyValue === 'object' ? keyValue[key] : keyValue;
                    return `${key}=${isString ? `'${value}'` : value}`;
                });
                keyPredicate = keyParts.join(',');
            }

            const url = `${this.serviceUrl}/${entitySetName}(${keyPredicate})`;

            const response = await fetch(url);
            if (!response.ok) {
                throw new Error(`Failed to fetch entity: ${response.statusText}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error fetching entity:', error);
            throw error;
        }
    }

    /**
     * Fetch a navigation property
     */
    async fetchNavigationProperty(entitySetName, keyValue, navigationProperty) {
        try {
            const entitySet = this.entitySets.get(entitySetName);
            const entityTypeInfo = entitySet.entityTypeInfo;

            // Build key predicate
            let keyPredicate;
            if (entityTypeInfo.keys.length === 1) {
                const key = entityTypeInfo.keys[0];
                const keyProp = entityTypeInfo.properties.find(p => p.name === key);
                const isString = keyProp?.type?.includes('String');
                keyPredicate = isString ? `'${keyValue}'` : keyValue;
            } else {
                const keyParts = entityTypeInfo.keys.map(key => {
                    const keyProp = entityTypeInfo.properties.find(p => p.name === key);
                    const isString = keyProp?.type?.includes('String');
                    const value = typeof keyValue === 'object' ? keyValue[key] : keyValue;
                    return `${key}=${isString ? `'${value}'` : value}`;
                });
                keyPredicate = keyParts.join(',');
            }

            const url = `${this.serviceUrl}/${entitySetName}(${keyPredicate})/${navigationProperty}`;

            const response = await fetch(url);
            if (!response.ok) {
                throw new Error(`Failed to fetch navigation property: ${response.statusText}`);
            }

            const data = await response.json();

            // Check if it's a collection or single entity
            if (data.value) {
                return { value: data.value, isCollection: true };
            } else {
                return { value: data, isCollection: false };
            }
        } catch (error) {
            console.error('Error fetching navigation property:', error);
            throw error;
        }
    }

    /**
     * Get entity type information
     */
    getEntityType(entityTypeName) {
        return this.entityTypes.get(entityTypeName);
    }

    /**
     * Get entity set information
     */
    getEntitySet(entitySetName) {
        return this.entitySets.get(entitySetName);
    }

    /**
     * Get all entity sets
     */
    getAllEntitySets() {
        return Array.from(this.entitySets.values());
    }

    /**
     * Get the primitive type category for a property
     */
    getPropertyTypeCategory(odataType) {
        if (!odataType) return 'string';

        const type = odataType.replace('Edm.', '').toLowerCase();

        if (type.includes('int') || type.includes('decimal') ||
            type.includes('double') || type.includes('single') ||
            type.includes('byte')) {
            return 'number';
        }

        if (type.includes('bool')) {
            return 'boolean';
        }

        if (type.includes('date') || type.includes('time')) {
            return 'datetime';
        }

        if (type.includes('guid')) {
            return 'guid';
        }

        return 'string';
    }

    /**
     * Extract key value(s) from an entity
     */
    extractKeyValue(entity, entityTypeInfo) {
        if (entityTypeInfo.keys.length === 1) {
            return entity[entityTypeInfo.keys[0]];
        } else {
            // Return composite key as object
            const keyObj = {};
            entityTypeInfo.keys.forEach(key => {
                keyObj[key] = entity[key];
            });
            return keyObj;
        }
    }

    /**
     * Get complex type information
     */
    getComplexType(complexTypeName) {
        return this.complexTypes.get(complexTypeName);
    }

    /**
     * Create a new entity (POST)
     */
    async createEntity(entitySetName, entityData) {
        try {
            const url = `${this.serviceUrl}/${entitySetName}`;

            const response = await fetch(url, {
                method: 'POST',
                headers: this.getHeaders(),
                body: JSON.stringify(entityData)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to create entity: ${response.statusText} - ${errorText}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error creating entity:', error);
            throw error;
        }
    }

    /**
     * Update an entity (PATCH)
     */
    async updateEntity(entitySetName, keyValue, entityData, entityTypeInfo) {
        try {
            // Build key predicate
            let keyPredicate;
            if (entityTypeInfo.keys.length === 1) {
                const key = entityTypeInfo.keys[0];
                const keyProp = entityTypeInfo.properties.find(p => p.name === key);
                const isString = keyProp?.type?.includes('String');
                keyPredicate = isString ? `'${keyValue}'` : keyValue;
            } else {
                const keyParts = entityTypeInfo.keys.map(key => {
                    const keyProp = entityTypeInfo.properties.find(p => p.name === key);
                    const isString = keyProp?.type?.includes('String');
                    const value = typeof keyValue === 'object' ? keyValue[key] : keyValue;
                    return `${key}=${isString ? `'${value}'` : value}`;
                });
                keyPredicate = keyParts.join(',');
            }

            const url = `${this.serviceUrl}/${entitySetName}(${keyPredicate})`;

            const response = await fetch(url, {
                method: 'PATCH',
                headers: this.getHeaders(),
                body: JSON.stringify(entityData)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to update entity: ${response.statusText} - ${errorText}`);
            }

            // PATCH may return 204 No Content or updated entity
            if (response.status === 204) {
                return entityData;
            }

            return await response.json();
        } catch (error) {
            console.error('Error updating entity:', error);
            throw error;
        }
    }

    /**
     * Delete an entity (DELETE)
     */
    async deleteEntity(entitySetName, keyValue, entityTypeInfo) {
        try {
            // Build key predicate
            let keyPredicate;
            if (entityTypeInfo.keys.length === 1) {
                const key = entityTypeInfo.keys[0];
                const keyProp = entityTypeInfo.properties.find(p => p.name === key);
                const isString = keyProp?.type?.includes('String');
                keyPredicate = isString ? `'${keyValue}'` : keyValue;
            } else {
                const keyParts = entityTypeInfo.keys.map(key => {
                    const keyProp = entityTypeInfo.properties.find(p => p.name === key);
                    const isString = keyProp?.type?.includes('String');
                    const value = typeof keyValue === 'object' ? keyValue[key] : keyValue;
                    return `${key}=${isString ? `'${value}'` : value}`;
                });
                keyPredicate = keyParts.join(',');
            }

            const url = `${this.serviceUrl}/${entitySetName}(${keyPredicate})`;

            const response = await fetch(url, {
                method: 'DELETE',
                headers: this.getHeaders()
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to delete entity: ${response.statusText} - ${errorText}`);
            }

            return true;
        } catch (error) {
            console.error('Error deleting entity:', error);
            throw error;
        }
    }

    /**
     * Check if property type is complex
     */
    isComplexType(typeName) {
        return this.complexTypes.has(typeName);
    }

    /**
     * Check if property type is primitive
     */
    isPrimitiveType(typeName) {
        if (!typeName) return true;
        return typeName.startsWith('Edm.') || typeName === 'String' || typeName === 'Int32' ||
               typeName === 'Boolean' || typeName === 'DateTime' || typeName === 'Decimal' ||
               typeName === 'Double' || typeName === 'Single' || typeName === 'Guid';
    }
}

// Export for use in app.js
window.ODataService = ODataService;
