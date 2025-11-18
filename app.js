/**
 * OData Explorer Application
 * Main application logic
 */

class ODataExplorer {
    constructor() {
        this.odataService = null;
        this.currentEntitySet = null;
        this.currentEntity = null;
        this.navigationStack = [];

        this.initializeEventListeners();
    }

    /**
     * Initialize all event listeners
     */
    initializeEventListeners() {
        // Connect button
        document.getElementById('connectBtn').addEventListener('click', () => {
            this.connectToService();
        });

        // Service URL input - connect on Enter key
        document.getElementById('serviceUrl').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.connectToService();
            }
        });

        // Tab buttons
        document.querySelectorAll('.tab-button').forEach(button => {
            button.addEventListener('click', (e) => {
                this.switchTab(e.target.dataset.tab);
            });
        });

        // Refresh button
        document.getElementById('refreshBtn').addEventListener('click', () => {
            if (this.currentEntitySet) {
                this.loadEntitySet(this.currentEntitySet);
            }
        });

        // Back button
        document.getElementById('backBtn').addEventListener('click', () => {
            this.navigateBack();
        });
    }

    /**
     * Connect to OData service
     */
    async connectToService() {
        const serviceUrl = document.getElementById('serviceUrl').value.trim();

        if (!serviceUrl) {
            alert('Please enter a valid OData service URL');
            return;
        }

        this.showLoading(true);

        try {
            this.odataService = new ODataService(serviceUrl);
            const metadata = await this.odataService.fetchMetadata();

            this.showConnectionStatus('Connected', true);
            this.buildEntityTree(metadata.entitySets);

            // Enable refresh button
            document.getElementById('refreshBtn').disabled = false;
        } catch (error) {
            this.showConnectionStatus(`Error: ${error.message}`, false);
            console.error('Connection error:', error);
        } finally {
            this.showLoading(false);
        }
    }

    /**
     * Build the entity set tree
     */
    buildEntityTree(entitySets) {
        const treeContainer = document.getElementById('entityTree');
        treeContainer.innerHTML = '';

        if (entitySets.length === 0) {
            treeContainer.innerHTML = '<p class="placeholder">No entity sets found</p>';
            return;
        }

        entitySets.forEach(entitySet => {
            const treeItem = document.createElement('div');
            treeItem.className = 'tree-item';
            treeItem.textContent = entitySet.name;
            treeItem.dataset.entitySet = entitySet.name;

            treeItem.addEventListener('click', () => {
                // Remove selection from all items
                document.querySelectorAll('.tree-item').forEach(item => {
                    item.classList.remove('selected');
                });

                // Select current item
                treeItem.classList.add('selected');

                // Load entity set
                this.loadEntitySet(entitySet.name);
            });

            treeContainer.appendChild(treeItem);
        });
    }

    /**
     * Load and display an entity set
     */
    async loadEntitySet(entitySetName) {
        this.showLoading(true);

        try {
            const result = await this.odataService.fetchEntitySet(entitySetName, { top: 25 });
            this.currentEntitySet = entitySetName;

            // Update grid title
            document.getElementById('gridTitle').textContent = entitySetName;
            document.getElementById('recordCount').textContent = `${result.value.length} of ${result.count} records`;

            // Build grid
            this.buildGrid(result.value, entitySetName);

            // Switch to grid tab
            this.switchTab('grid');

            // Clear detail view
            this.clearDetailView();
        } catch (error) {
            this.showError('Failed to load entity set', error);
        } finally {
            this.showLoading(false);
        }
    }

    /**
     * Build a data grid
     */
    buildGrid(data, entitySetName, container = null) {
        const gridContainer = container || document.getElementById('gridContainer');
        gridContainer.innerHTML = '';

        if (!data || data.length === 0) {
            gridContainer.innerHTML = '<p class="placeholder">No data available</p>';
            return;
        }

        // Get entity set info
        const entitySet = this.odataService.getEntitySet(entitySetName);
        const entityTypeInfo = entitySet?.entityTypeInfo;

        // Create table
        const table = document.createElement('table');
        table.className = 'data-table';

        // Create header
        const thead = document.createElement('thead');
        const headerRow = document.createElement('tr');

        // Get columns from first row and entity type info
        const columns = this.getGridColumns(data[0], entityTypeInfo);

        columns.forEach(col => {
            const th = document.createElement('th');
            th.textContent = col;
            headerRow.appendChild(th);
        });

        thead.appendChild(headerRow);
        table.appendChild(thead);

        // Create body
        const tbody = document.createElement('tbody');

        data.forEach(row => {
            const tr = document.createElement('tr');

            columns.forEach(col => {
                const td = document.createElement('td');
                const value = row[col];
                td.textContent = this.formatCellValue(value);
                tr.appendChild(td);
            });

            // Add click handler to show detail
            tr.addEventListener('click', () => {
                // Remove previous selection
                tbody.querySelectorAll('tr').forEach(r => r.classList.remove('selected'));
                tr.classList.add('selected');

                // Load entity detail
                this.loadEntityDetail(entitySetName, row, entityTypeInfo);
            });

            tbody.appendChild(tr);
        });

        table.appendChild(tbody);
        gridContainer.appendChild(table);
    }

    /**
     * Get columns for grid display
     */
    getGridColumns(sampleRow, entityTypeInfo) {
        if (!sampleRow) return [];

        const columns = [];

        // First add key columns
        if (entityTypeInfo?.keys) {
            entityTypeInfo.keys.forEach(key => {
                if (sampleRow.hasOwnProperty(key)) {
                    columns.push(key);
                }
            });
        }

        // Then add other simple properties (not objects or arrays)
        Object.keys(sampleRow).forEach(key => {
            if (!columns.includes(key) &&
                !key.startsWith('@') &&
                !key.startsWith('odata') &&
                typeof sampleRow[key] !== 'object') {
                columns.push(key);
            }
        });

        return columns;
    }

    /**
     * Format cell value for display
     */
    formatCellValue(value) {
        if (value === null || value === undefined) {
            return '';
        }

        if (typeof value === 'boolean') {
            return value ? 'Yes' : 'No';
        }

        if (typeof value === 'object') {
            if (value instanceof Date) {
                return value.toLocaleString();
            }
            return '[Object]';
        }

        if (typeof value === 'string' && value.length > 100) {
            return value.substring(0, 100) + '...';
        }

        return String(value);
    }

    /**
     * Load entity detail view
     */
    async loadEntityDetail(entitySetName, entity, entityTypeInfo) {
        this.showLoading(true);

        try {
            // Extract key value
            const keyValue = this.odataService.extractKeyValue(entity, entityTypeInfo);

            // Fetch full entity from server
            const fullEntity = await this.odataService.fetchEntity(entitySetName, keyValue);

            this.currentEntity = {
                entitySetName,
                entity: fullEntity,
                entityTypeInfo
            };

            // Build detail form
            this.buildDetailForm(fullEntity, entityTypeInfo, entitySetName);

            // Switch to detail tab
            this.switchTab('detail');
        } catch (error) {
            this.showError('Failed to load entity detail', error);
        } finally {
            this.showLoading(false);
        }
    }

    /**
     * Build detail form
     */
    buildDetailForm(entity, entityTypeInfo, entitySetName) {
        const detailContainer = document.getElementById('detailContainer');
        detailContainer.innerHTML = '';

        // Update title
        const keyValue = this.odataService.extractKeyValue(entity, entityTypeInfo);
        document.getElementById('detailTitle').textContent =
            `${entitySetName} - ${typeof keyValue === 'object' ? JSON.stringify(keyValue) : keyValue}`;

        // Create form
        const form = document.createElement('div');
        form.className = 'detail-form';

        // Properties Section
        const propsSection = document.createElement('div');
        propsSection.className = 'form-section';
        propsSection.innerHTML = '<h3>Properties</h3>';

        entityTypeInfo.properties.forEach(prop => {
            const formGroup = this.createFormField(prop, entity[prop.name], entityTypeInfo);
            propsSection.appendChild(formGroup);
        });

        form.appendChild(propsSection);

        // Navigation Properties Section
        if (entityTypeInfo.navigationProperties && entityTypeInfo.navigationProperties.length > 0) {
            const navSection = document.createElement('div');
            navSection.className = 'form-section';
            navSection.innerHTML = '<h3>Related Data</h3>';

            entityTypeInfo.navigationProperties.forEach(navProp => {
                const navGroup = this.createNavigationField(
                    navProp,
                    entity,
                    entitySetName,
                    entityTypeInfo
                );
                navSection.appendChild(navGroup);
            });

            form.appendChild(navSection);
        }

        detailContainer.appendChild(form);
    }

    /**
     * Create a form field based on property metadata
     */
    createFormField(property, value, entityTypeInfo) {
        const formGroup = document.createElement('div');
        formGroup.className = 'form-group';

        // Add type classes
        if (entityTypeInfo.keys.includes(property.name)) {
            formGroup.classList.add('type-key');
        }
        if (!property.nullable) {
            formGroup.classList.add('type-required');
        }

        const label = document.createElement('label');
        label.textContent = property.name;
        formGroup.appendChild(label);

        const typeCategory = this.odataService.getPropertyTypeCategory(property.type);

        let input;

        switch (typeCategory) {
            case 'boolean':
                input = document.createElement('div');
                input.className = 'checkbox-group';
                const checkbox = document.createElement('input');
                checkbox.type = 'checkbox';
                checkbox.checked = value === true;
                checkbox.disabled = true;
                const checkboxLabel = document.createElement('span');
                checkboxLabel.textContent = value ? 'True' : 'False';
                input.appendChild(checkbox);
                input.appendChild(checkboxLabel);
                break;

            case 'number':
                input = document.createElement('input');
                input.type = 'number';
                input.value = value !== null && value !== undefined ? value : '';
                input.readOnly = true;
                break;

            case 'datetime':
                input = document.createElement('input');
                input.type = 'datetime-local';
                if (value) {
                    try {
                        const date = new Date(value);
                        input.value = date.toISOString().slice(0, 16);
                    } catch (e) {
                        input.value = value;
                    }
                }
                input.readOnly = true;
                break;

            default:
                // String and other types
                if (property.maxLength && property.maxLength > 200) {
                    input = document.createElement('textarea');
                    input.value = value !== null && value !== undefined ? value : '';
                    input.readOnly = true;
                } else {
                    input = document.createElement('input');
                    input.type = 'text';
                    input.value = value !== null && value !== undefined ? value : '';
                    input.readOnly = true;
                }
                break;
        }

        formGroup.appendChild(input);

        // Add type hint
        const typeHint = document.createElement('small');
        typeHint.style.color = '#7f8c8d';
        typeHint.style.fontSize = '0.8rem';
        typeHint.textContent = `Type: ${property.type}`;
        formGroup.appendChild(typeHint);

        return formGroup;
    }

    /**
     * Create a navigation property field
     */
    createNavigationField(navProp, entity, entitySetName, entityTypeInfo) {
        const formGroup = document.createElement('div');
        formGroup.className = 'form-group type-navigation';

        const label = document.createElement('label');
        label.textContent = navProp.name;
        formGroup.appendChild(label);

        const button = document.createElement('button');
        button.textContent = navProp.isCollection ? 'View Collection' : 'View Related Entity';
        button.style.width = 'auto';

        button.addEventListener('click', async () => {
            await this.loadNavigationProperty(
                entitySetName,
                entity,
                navProp,
                entityTypeInfo
            );
        });

        formGroup.appendChild(button);

        // Add type hint
        const typeHint = document.createElement('small');
        typeHint.style.color = '#7f8c8d';
        typeHint.style.fontSize = '0.8rem';
        typeHint.style.display = 'block';
        typeHint.textContent = `Type: ${navProp.type}`;
        formGroup.appendChild(typeHint);

        return formGroup;
    }

    /**
     * Load navigation property data
     */
    async loadNavigationProperty(entitySetName, entity, navProp, entityTypeInfo) {
        this.showLoading(true);

        try {
            const keyValue = this.odataService.extractKeyValue(entity, entityTypeInfo);
            const result = await this.odataService.fetchNavigationProperty(
                entitySetName,
                keyValue,
                navProp.name
            );

            // Store current state in navigation stack
            this.navigationStack.push({
                type: 'entity',
                entitySetName,
                entity,
                entityTypeInfo
            });

            if (result.isCollection) {
                // Display as grid in a new view
                this.showNavigationGrid(navProp.name, result.value, navProp.type);
            } else {
                // Display as form
                // Extract entity type name from navProp.type
                const targetEntityTypeName = navProp.type.replace('Collection(', '').replace(')', '');
                const targetEntityTypeInfo = this.odataService.getEntityType(targetEntityTypeName);

                if (targetEntityTypeInfo) {
                    this.buildDetailForm(result.value, targetEntityTypeInfo, navProp.name);
                }
            }
        } catch (error) {
            this.showError('Failed to load navigation property', error);
        } finally {
            this.showLoading(false);
        }
    }

    /**
     * Show navigation grid
     */
    showNavigationGrid(propertyName, data, typeInfo) {
        const detailContainer = document.getElementById('detailContainer');
        detailContainer.innerHTML = '';

        // Update title
        document.getElementById('detailTitle').textContent = propertyName;

        // Extract entity type from Collection(Namespace.EntityType)
        const entityTypeName = typeInfo.replace('Collection(', '').replace(')', '');
        const entityTypeInfo = this.odataService.getEntityType(entityTypeName);

        if (!data || data.length === 0) {
            detailContainer.innerHTML = '<p class="placeholder">No related data</p>';
            return;
        }

        // Create container for grid
        const gridDiv = document.createElement('div');
        gridDiv.className = 'nested-grid';

        // Create table
        const table = document.createElement('table');
        table.className = 'data-table';

        // Create header
        const thead = document.createElement('thead');
        const headerRow = document.createElement('tr');

        const columns = this.getGridColumns(data[0], entityTypeInfo);

        columns.forEach(col => {
            const th = document.createElement('th');
            th.textContent = col;
            headerRow.appendChild(th);
        });

        thead.appendChild(headerRow);
        table.appendChild(thead);

        // Create body
        const tbody = document.createElement('tbody');

        data.forEach(row => {
            const tr = document.createElement('tr');

            columns.forEach(col => {
                const td = document.createElement('td');
                const value = row[col];
                td.textContent = this.formatCellValue(value);
                tr.appendChild(td);
            });

            // Add click handler
            tr.addEventListener('click', () => {
                tbody.querySelectorAll('tr').forEach(r => r.classList.remove('selected'));
                tr.classList.add('selected');

                // Store current grid state
                this.navigationStack.push({
                    type: 'grid',
                    propertyName,
                    data,
                    typeInfo
                });

                // Show detail form for this row
                this.buildDetailForm(row, entityTypeInfo, propertyName);
            });

            tbody.appendChild(tr);
        });

        table.appendChild(tbody);
        gridDiv.appendChild(table);
        detailContainer.appendChild(gridDiv);
    }

    /**
     * Navigate back in navigation stack
     */
    navigateBack() {
        if (this.navigationStack.length === 0) {
            // Go back to grid view
            this.switchTab('grid');
            return;
        }

        const previousState = this.navigationStack.pop();

        if (previousState.type === 'entity') {
            this.buildDetailForm(
                previousState.entity,
                previousState.entityTypeInfo,
                previousState.entitySetName
            );
        } else if (previousState.type === 'grid') {
            this.showNavigationGrid(
                previousState.propertyName,
                previousState.data,
                previousState.typeInfo
            );
        }
    }

    /**
     * Clear detail view
     */
    clearDetailView() {
        document.getElementById('detailContainer').innerHTML =
            '<p class="placeholder">Select a row from the grid to view details</p>';
        document.getElementById('detailTitle').textContent = 'Detail View';
        this.navigationStack = [];
    }

    /**
     * Switch between tabs
     */
    switchTab(tabName) {
        // Update tab buttons
        document.querySelectorAll('.tab-button').forEach(button => {
            button.classList.remove('active');
            if (button.dataset.tab === tabName) {
                button.classList.add('active');
            }
        });

        // Update tab panes
        document.querySelectorAll('.tab-pane').forEach(pane => {
            pane.classList.remove('active');
        });

        const targetPane = tabName === 'grid' ?
            document.getElementById('gridTab') :
            document.getElementById('detailTab');
        targetPane.classList.add('active');
    }

    /**
     * Show/hide loading overlay
     */
    showLoading(show) {
        const overlay = document.getElementById('loadingOverlay');
        if (show) {
            overlay.classList.remove('hidden');
        } else {
            overlay.classList.add('hidden');
        }
    }

    /**
     * Show connection status
     */
    showConnectionStatus(message, success) {
        const statusElement = document.getElementById('connectionStatus');
        statusElement.textContent = message;
        statusElement.style.color = success ? '#2ecc71' : '#e74c3c';
    }

    /**
     * Show error message
     */
    showError(title, error) {
        alert(`${title}\n\n${error.message}`);
        console.error(title, error);
    }
}

// Initialize application when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.app = new ODataExplorer();

    // Set default service URL for testing
    const defaultUrl = 'https://services.odata.org/V4/TripPinServiceRW';
    document.getElementById('serviceUrl').value = defaultUrl;
});
