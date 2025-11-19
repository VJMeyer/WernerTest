/**
 * OData Explorer Application
 * Main application logic
 */

class ODataExplorer {
    constructor() {
        this.odataService = null;
        this.authService = null;
        this.currentEntitySet = null;
        this.currentEntity = null;
        this.currentEntityTypeInfo = null;
        this.navigationStack = [];

        // Pagination state
        this.currentPage = 1;
        this.pageSize = 25;
        this.totalCount = 0;
        this.totalPages = 0;

        // Filter state
        this.activeFilters = [];

        // Edit mode state
        this.isEditMode = false;
        this.isCreateMode = false;
        this.originalEntityData = null;

        // Complex type state
        this.currentComplexTypeData = null;
        this.currentComplexTypeProperty = null;

        this.initializeAuthentication();
        this.initializeEventListeners();
    }

    /**
     * Initialize authentication
     */
    initializeAuthentication() {
        // Load auth config from localStorage
        const authConfig = localStorage.getItem('keycloak_config');
        if (authConfig) {
            const config = JSON.parse(authConfig);
            this.authService = new AuthService();
            this.authService.initialize(config);

            // Update UI based on auth state
            this.updateAuthUI();
        }
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

        // Filter controls
        document.getElementById('showFiltersBtn').addEventListener('click', () => {
            this.toggleFilterPanel();
        });

        document.getElementById('toggleFilterBtn').addEventListener('click', () => {
            this.toggleFilterPanel();
        });

        document.getElementById('applyFilterBtn').addEventListener('click', () => {
            this.applyFilter();
        });

        document.getElementById('clearFilterBtn').addEventListener('click', () => {
            this.clearFilters();
        });

        document.getElementById('filterValue').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.applyFilter();
            }
        });

        // Pagination controls - Top
        document.getElementById('firstPageBtn').addEventListener('click', () => {
            this.goToPage(1);
        });

        document.getElementById('prevPageBtn').addEventListener('click', () => {
            this.goToPage(this.currentPage - 1);
        });

        document.getElementById('nextPageBtn').addEventListener('click', () => {
            this.goToPage(this.currentPage + 1);
        });

        document.getElementById('lastPageBtn').addEventListener('click', () => {
            this.goToPage(this.totalPages);
        });

        // Pagination controls - Bottom
        document.getElementById('firstPageBtnBottom').addEventListener('click', () => {
            this.goToPage(1);
        });

        document.getElementById('prevPageBtnBottom').addEventListener('click', () => {
            this.goToPage(this.currentPage - 1);
        });

        document.getElementById('nextPageBtnBottom').addEventListener('click', () => {
            this.goToPage(this.currentPage + 1);
        });

        document.getElementById('lastPageBtnBottom').addEventListener('click', () => {
            this.goToPage(this.totalPages);
        });

        // Page size selector
        document.getElementById('pageSizeSelect').addEventListener('change', (e) => {
            this.pageSize = parseInt(e.target.value);
            this.currentPage = 1;
            if (this.currentEntitySet) {
                this.loadEntitySet(this.currentEntitySet);
            }
        });

        // Authentication event listeners
        document.getElementById('authConfigBtn').addEventListener('click', () => {
            this.showAuthConfigModal();
        });

        document.getElementById('loginBtn').addEventListener('click', () => {
            this.login();
        });

        document.getElementById('logoutBtn').addEventListener('click', () => {
            this.logout();
        });

        document.getElementById('authConfigCloseBtn').addEventListener('click', () => {
            this.hideAuthConfigModal();
        });

        document.getElementById('saveAuthConfigBtn').addEventListener('click', () => {
            this.saveAuthConfig();
        });

        document.getElementById('cancelAuthConfigBtn').addEventListener('click', () => {
            this.hideAuthConfigModal();
        });

        // CRUD event listeners
        document.getElementById('createEntityBtn').addEventListener('click', () => {
            this.createNewEntity();
        });

        document.getElementById('editBtn').addEventListener('click', () => {
            this.enterEditMode();
        });

        document.getElementById('saveBtn').addEventListener('click', () => {
            this.saveEntity();
        });

        document.getElementById('cancelEditBtn').addEventListener('click', () => {
            this.cancelEdit();
        });

        document.getElementById('deleteBtn').addEventListener('click', () => {
            this.deleteEntity();
        });

        // Complex type modal
        document.getElementById('complexTypeCloseBtn').addEventListener('click', () => {
            this.hideComplexTypeModal();
        });

        document.getElementById('saveComplexTypeBtn').addEventListener('click', () => {
            this.saveComplexType();
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
            this.odataService = new ODataService(serviceUrl, this.authService);
            const metadata = await this.odataService.fetchMetadata();

            this.showConnectionStatus('Connected', true);
            this.buildEntityTree(metadata.entitySets);

            // Enable buttons
            document.getElementById('refreshBtn').disabled = false;
            document.getElementById('showFiltersBtn').disabled = false;

            // Enable create button if authenticated
            if (this.isAuthenticated()) {
                document.getElementById('createEntityBtn').disabled = false;
            }
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
            // Build filter expression
            const filter = this.buildFilterExpression();

            // Calculate skip
            const skip = (this.currentPage - 1) * this.pageSize;

            const result = await this.odataService.fetchEntitySet(entitySetName, {
                top: this.pageSize,
                skip: skip,
                filter: filter
            });

            this.currentEntitySet = entitySetName;
            this.totalCount = result.count;
            this.totalPages = Math.ceil(this.totalCount / this.pageSize);

            // Update grid title
            document.getElementById('gridTitle').textContent = entitySetName;
            document.getElementById('recordCount').textContent = `${result.value.length} of ${result.count} records`;

            // Build grid
            this.buildGrid(result.value, entitySetName);

            // Update filter property dropdown
            this.buildFilterPropertyDropdown(entitySetName);

            // Update pagination controls
            this.updatePaginationControls();

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
    buildDetailForm(entity, entityTypeInfo, entitySetName, isNewEntity = false) {
        const detailContainer = document.getElementById('detailContainer');
        detailContainer.innerHTML = '';

        // Update title
        if (!isNewEntity) {
            const keyValue = this.odataService.extractKeyValue(entity, entityTypeInfo);
            document.getElementById('detailTitle').textContent =
                `${entitySetName} - ${typeof keyValue === 'object' ? JSON.stringify(keyValue) : keyValue}`;
        } else {
            document.getElementById('detailTitle').textContent = `Create New ${entitySetName}`;
        }

        // Show action buttons
        document.getElementById('detailActions').classList.remove('hidden');

        // Update action buttons based on mode and authentication
        if (isNewEntity || this.isEditMode) {
            document.getElementById('editBtn').classList.add('hidden');
            document.getElementById('saveBtn').classList.remove('hidden');
            document.getElementById('cancelEditBtn').classList.remove('hidden');
            document.getElementById('deleteBtn').classList.add('hidden');
        } else {
            const isAuth = this.isAuthenticated();
            document.getElementById('editBtn').classList.toggle('hidden', !isAuth);
            document.getElementById('saveBtn').classList.add('hidden');
            document.getElementById('cancelEditBtn').classList.add('hidden');
            document.getElementById('deleteBtn').classList.toggle('hidden', !isAuth);
        }

        // Create form
        const form = document.createElement('div');
        form.className = 'detail-form';

        // Properties Section
        const propsSection = document.createElement('div');
        propsSection.className = 'form-section';
        propsSection.innerHTML = '<h3>Properties</h3>';

        entityTypeInfo.properties.forEach(prop => {
            const formGroup = this.createFormField(prop, entity[prop.name], entityTypeInfo, isNewEntity || this.isEditMode);
            propsSection.appendChild(formGroup);
        });

        form.appendChild(propsSection);

        // Navigation Properties Section (only show in view mode, not edit/create)
        if (!isNewEntity && !this.isEditMode && entityTypeInfo.navigationProperties && entityTypeInfo.navigationProperties.length > 0) {
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
    createFormField(property, value, entityTypeInfo, isEditable = false) {
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

        // Handle complex types
        if (property.isComplexType) {
            const complexTypeInfo = this.odataService.getComplexType(property.type);

            const complexField = document.createElement('div');
            complexField.className = 'complex-type-field';

            if (value && typeof value === 'object') {
                const pre = document.createElement('pre');
                pre.textContent = JSON.stringify(value, null, 2);
                complexField.appendChild(pre);
            } else {
                complexField.innerHTML = '<em>No data</em>';
            }

            formGroup.appendChild(complexField);

            if (isEditable && complexTypeInfo) {
                const editButton = document.createElement('button');
                editButton.className = 'complex-type-button';
                editButton.textContent = 'Edit';
                editButton.type = 'button';
                editButton.addEventListener('click', () => {
                    this.showComplexTypeModal(property.name, complexTypeInfo, value);
                });
                formGroup.appendChild(editButton);
            }

            return formGroup;
        }

        const typeCategory = this.odataService.getPropertyTypeCategory(property.type);
        let input;

        switch (typeCategory) {
            case 'boolean':
                input = document.createElement('div');
                input.className = 'checkbox-group';
                const checkbox = document.createElement('input');
                checkbox.type = 'checkbox';
                checkbox.checked = value === true;
                checkbox.disabled = !isEditable;
                const checkboxLabel = document.createElement('span');
                checkboxLabel.textContent = value ? 'True' : 'False';
                input.appendChild(checkbox);
                input.appendChild(checkboxLabel);
                checkbox.addEventListener('change', () => {
                    checkboxLabel.textContent = checkbox.checked ? 'True' : 'False';
                });
                break;

            case 'number':
                input = document.createElement('input');
                input.type = 'number';
                input.value = value !== null && value !== undefined ? value : '';
                input.readOnly = !isEditable;
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
                input.readOnly = !isEditable;
                break;

            default:
                // String and other types
                if (property.maxLength && property.maxLength > 200) {
                    input = document.createElement('textarea');
                    input.value = value !== null && value !== undefined ? value : '';
                    input.readOnly = !isEditable;
                } else {
                    input = document.createElement('input');
                    input.type = 'text';
                    input.value = value !== null && value !== undefined ? value : '';
                    input.readOnly = !isEditable;
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

    /**
     * Toggle filter panel visibility
     */
    toggleFilterPanel() {
        const filterPanel = document.getElementById('filterPanel');
        const toggleBtn = document.getElementById('toggleFilterBtn');

        if (filterPanel.classList.contains('hidden')) {
            filterPanel.classList.remove('hidden');
            toggleBtn.textContent = 'Hide Filters';
        } else {
            filterPanel.classList.add('hidden');
            toggleBtn.textContent = 'Show Filters';
        }
    }

    /**
     * Build filter property dropdown
     */
    buildFilterPropertyDropdown(entitySetName) {
        const entitySet = this.odataService.getEntitySet(entitySetName);
        const entityTypeInfo = entitySet?.entityTypeInfo;

        if (!entityTypeInfo) return;

        const propertySelect = document.getElementById('filterProperty');
        propertySelect.innerHTML = '<option value="">Select property...</option>';

        entityTypeInfo.properties.forEach(prop => {
            const option = document.createElement('option');
            option.value = prop.name;
            option.textContent = `${prop.name} (${prop.type})`;
            option.dataset.type = prop.type;
            propertySelect.appendChild(option);
        });
    }

    /**
     * Apply filter
     */
    applyFilter() {
        const property = document.getElementById('filterProperty').value;
        const operator = document.getElementById('filterOperator').value;
        const value = document.getElementById('filterValue').value;

        if (!property || !value) {
            alert('Please select a property and enter a value');
            return;
        }

        // Get property type
        const propertyOption = document.querySelector(`#filterProperty option[value="${property}"]`);
        const propertyType = propertyOption?.dataset.type || 'Edm.String';

        // Check if filter already exists for this property
        const existingIndex = this.activeFilters.findIndex(f => f.property === property && f.operator === operator);
        if (existingIndex >= 0) {
            this.activeFilters[existingIndex].value = value;
        } else {
            this.activeFilters.push({ property, operator, value, propertyType });
        }

        // Reset to first page
        this.currentPage = 1;

        // Reload data
        if (this.currentEntitySet) {
            this.loadEntitySet(this.currentEntitySet);
        }

        // Clear input
        document.getElementById('filterValue').value = '';

        // Update active filters display
        this.displayActiveFilters();
    }

    /**
     * Clear all filters
     */
    clearFilters() {
        this.activeFilters = [];
        this.currentPage = 1;

        if (this.currentEntitySet) {
            this.loadEntitySet(this.currentEntitySet);
        }

        document.getElementById('filterProperty').value = '';
        document.getElementById('filterValue').value = '';

        this.displayActiveFilters();
    }

    /**
     * Remove a specific filter
     */
    removeFilter(index) {
        this.activeFilters.splice(index, 1);
        this.currentPage = 1;

        if (this.currentEntitySet) {
            this.loadEntitySet(this.currentEntitySet);
        }

        this.displayActiveFilters();
    }

    /**
     * Display active filters
     */
    displayActiveFilters() {
        const container = document.getElementById('activeFilters');
        container.innerHTML = '';

        if (this.activeFilters.length === 0) {
            return;
        }

        this.activeFilters.forEach((filter, index) => {
            const badge = document.createElement('div');
            badge.className = 'filter-badge';

            const operatorText = this.getOperatorText(filter.operator);
            badge.innerHTML = `
                <span>${filter.property} ${operatorText} "${filter.value}"</span>
                <button onclick="app.removeFilter(${index})">×</button>
            `;

            container.appendChild(badge);
        });
    }

    /**
     * Get human-readable operator text
     */
    getOperatorText(operator) {
        const operators = {
            'eq': '=',
            'ne': '≠',
            'gt': '>',
            'ge': '≥',
            'lt': '<',
            'le': '≤',
            'contains': 'contains',
            'startswith': 'starts with',
            'endswith': 'ends with'
        };
        return operators[operator] || operator;
    }

    /**
     * Build OData filter expression
     */
    buildFilterExpression() {
        if (this.activeFilters.length === 0) {
            return null;
        }

        const filterExpressions = this.activeFilters.map(filter => {
            const isString = filter.propertyType.includes('String');
            let expression;

            if (filter.operator === 'contains' || filter.operator === 'startswith' || filter.operator === 'endswith') {
                // Function-based operators
                expression = `${filter.operator}(${filter.property}, '${filter.value}')`;
            } else {
                // Comparison operators
                const formattedValue = isString ? `'${filter.value}'` : filter.value;
                expression = `${filter.property} ${filter.operator} ${formattedValue}`;
            }

            return expression;
        });

        return filterExpressions.join(' and ');
    }

    /**
     * Go to specific page
     */
    goToPage(page) {
        if (page < 1 || page > this.totalPages || page === this.currentPage) {
            return;
        }

        this.currentPage = page;

        if (this.currentEntitySet) {
            this.loadEntitySet(this.currentEntitySet);
        }
    }

    /**
     * Update pagination controls
     */
    updatePaginationControls() {
        // Show pagination containers
        document.getElementById('paginationTop').classList.remove('hidden');
        document.getElementById('paginationBottom').classList.remove('hidden');

        // Update page info
        const pageInfo = `Page ${this.currentPage} of ${this.totalPages}`;
        document.getElementById('pageInfo').textContent = pageInfo;
        document.getElementById('pageInfoBottom').textContent = pageInfo;

        // Update buttons state
        const isFirstPage = this.currentPage === 1;
        const isLastPage = this.currentPage === this.totalPages;

        // Top pagination buttons
        document.getElementById('firstPageBtn').disabled = isFirstPage;
        document.getElementById('prevPageBtn').disabled = isFirstPage;
        document.getElementById('nextPageBtn').disabled = isLastPage;
        document.getElementById('lastPageBtn').disabled = isLastPage;

        // Bottom pagination buttons
        document.getElementById('firstPageBtnBottom').disabled = isFirstPage;
        document.getElementById('prevPageBtnBottom').disabled = isFirstPage;
        document.getElementById('nextPageBtnBottom').disabled = isLastPage;
        document.getElementById('lastPageBtnBottom').disabled = isLastPage;

        // Build page numbers
        this.buildPageNumbers();
    }

    /**
     * Build page number buttons
     */
    buildPageNumbers() {
        const topContainer = document.getElementById('pageNumbers');
        const bottomContainer = document.getElementById('pageNumbersBottom');

        topContainer.innerHTML = '';
        bottomContainer.innerHTML = '';

        if (this.totalPages <= 1) {
            return;
        }

        const maxPageButtons = 7;
        let startPage = Math.max(1, this.currentPage - Math.floor(maxPageButtons / 2));
        let endPage = Math.min(this.totalPages, startPage + maxPageButtons - 1);

        // Adjust start if we're near the end
        if (endPage - startPage < maxPageButtons - 1) {
            startPage = Math.max(1, endPage - maxPageButtons + 1);
        }

        // Add first page and ellipsis if needed
        if (startPage > 1) {
            this.addPageButton(topContainer, 1);
            this.addPageButton(bottomContainer, 1);

            if (startPage > 2) {
                this.addEllipsis(topContainer);
                this.addEllipsis(bottomContainer);
            }
        }

        // Add page buttons
        for (let i = startPage; i <= endPage; i++) {
            this.addPageButton(topContainer, i);
            this.addPageButton(bottomContainer, i);
        }

        // Add last page and ellipsis if needed
        if (endPage < this.totalPages) {
            if (endPage < this.totalPages - 1) {
                this.addEllipsis(topContainer);
                this.addEllipsis(bottomContainer);
            }

            this.addPageButton(topContainer, this.totalPages);
            this.addPageButton(bottomContainer, this.totalPages);
        }
    }

    /**
     * Add page number button
     */
    addPageButton(container, pageNum) {
        const button = document.createElement('div');
        button.className = 'page-number';
        button.textContent = pageNum;

        if (pageNum === this.currentPage) {
            button.classList.add('active');
        }

        button.addEventListener('click', () => {
            this.goToPage(pageNum);
        });

        container.appendChild(button);
    }

    /**
     * Add ellipsis to page numbers
     */
    addEllipsis(container) {
        const ellipsis = document.createElement('div');
        ellipsis.className = 'page-number ellipsis';
        ellipsis.textContent = '...';
        container.appendChild(ellipsis);
    }

    // ========== Authentication Methods ==========

    /**
     * Check if user is authenticated
     */
    isAuthenticated() {
        return this.authService && this.authService.isAuthenticated();
    }

    /**
     * Update authentication UI
     */
    updateAuthUI() {
        const loginBtn = document.getElementById('loginBtn');
        const logoutBtn = document.getElementById('logoutBtn');
        const userInfo = document.getElementById('userInfo');

        if (this.isAuthenticated()) {
            loginBtn.classList.add('hidden');
            logoutBtn.classList.remove('hidden');

            const user = this.authService.getUserInfo();
            if (user) {
                userInfo.textContent = user.preferred_username || user.name || 'User';
                userInfo.classList.remove('hidden');
            }

            // Enable create button if service is connected
            if (this.odataService) {
                document.getElementById('createEntityBtn').disabled = false;
            }
        } else {
            loginBtn.classList.remove('hidden');
            logoutBtn.classList.add('hidden');
            userInfo.classList.add('hidden');
            document.getElementById('createEntityBtn').disabled = true;
        }
    }

    /**
     * Show authentication configuration modal
     */
    showAuthConfigModal() {
        const authConfig = localStorage.getItem('keycloak_config');
        if (authConfig) {
            const config = JSON.parse(authConfig);
            document.getElementById('keycloakRealm').value = config.realm || '';
            document.getElementById('keycloakClientId').value = config.clientId || '';
            document.getElementById('keycloakBaseUrl').value = config.baseUrl || '';
        }

        document.getElementById('authConfigModal').classList.remove('hidden');
    }

    /**
     * Hide authentication configuration modal
     */
    hideAuthConfigModal() {
        document.getElementById('authConfigModal').classList.add('hidden');
    }

    /**
     * Save authentication configuration
     */
    saveAuthConfig() {
        const realm = document.getElementById('keycloakRealm').value.trim();
        const clientId = document.getElementById('keycloakClientId').value.trim();
        const baseUrl = document.getElementById('keycloakBaseUrl').value.trim();

        if (!realm || !clientId) {
            alert('Please enter both Realm and Client ID');
            return;
        }

        const config = {
            realm,
            clientId,
            baseUrl: baseUrl || window.location.origin
        };

        localStorage.setItem('keycloak_config', JSON.stringify(config));

        // Initialize auth service
        this.authService = new AuthService();
        this.authService.initialize(config);

        // Update OData service if connected
        if (this.odataService) {
            this.odataService.setAuthService(this.authService);
        }

        this.updateAuthUI();
        this.hideAuthConfigModal();

        alert('Authentication configuration saved. You can now login.');
    }

    /**
     * Login
     */
    login() {
        if (!this.authService) {
            alert('Please configure authentication first (click the ⚙️ button)');
            return;
        }

        this.authService.login();
    }

    /**
     * Logout
     */
    logout() {
        if (this.authService) {
            this.authService.logout();
        }
    }

    // ========== CRUD Methods ==========

    /**
     * Create new entity
     */
    createNewEntity() {
        if (!this.isAuthenticated()) {
            alert('Please login to create entities');
            return;
        }

        if (!this.currentEntitySet) {
            alert('Please select an entity set first');
            return;
        }

        this.isCreateMode = true;
        this.isEditMode = true;

        const entitySet = this.odataService.getEntitySet(this.currentEntitySet);
        const entityTypeInfo = entitySet?.entityTypeInfo;

        if (!entityTypeInfo) {
            alert('Could not load entity type information');
            return;
        }

        // Create empty entity with default values
        const newEntity = {};
        entityTypeInfo.properties.forEach(prop => {
            if (!prop.isComplexType) {
                newEntity[prop.name] = this.getDefaultValue(prop);
            } else {
                newEntity[prop.name] = null;
            }
        });

        this.currentEntity = { entitySetName: this.currentEntitySet, entity: newEntity, entityTypeInfo };
        this.buildDetailForm(newEntity, entityTypeInfo, this.currentEntitySet, true);

        // Switch to detail tab
        this.switchTab('detail');

        // Update action buttons for create mode
        document.getElementById('detailActions').classList.remove('hidden');
        document.getElementById('editBtn').classList.add('hidden');
        document.getElementById('saveBtn').classList.remove('hidden');
        document.getElementById('cancelEditBtn').classList.remove('hidden');
        document.getElementById('deleteBtn').classList.add('hidden');

        document.getElementById('detailTitle').textContent = `Create New ${this.currentEntitySet}`;
    }

    /**
     * Get default value for a property based on type
     */
    getDefaultValue(property) {
        const typeCategory = this.odataService.getPropertyTypeCategory(property.type);

        switch (typeCategory) {
            case 'number':
                return 0;
            case 'boolean':
                return false;
            case 'datetime':
                return new Date().toISOString();
            default:
                return '';
        }
    }

    /**
     * Enter edit mode
     */
    enterEditMode() {
        if (!this.isAuthenticated()) {
            alert('Please login to edit entities');
            return;
        }

        this.isEditMode = true;
        this.originalEntityData = JSON.parse(JSON.stringify(this.currentEntity.entity));

        // Make all inputs editable
        const form = document.querySelector('#detailContainer .detail-form');
        if (form) {
            form.querySelectorAll('input, textarea, select').forEach(input => {
                if (input.type !== 'checkbox') {
                    input.removeAttribute('readonly');
                } else {
                    input.removeAttribute('disabled');
                }
                input.classList.remove('readonly');
            });
        }

        // Update action buttons
        document.getElementById('editBtn').classList.add('hidden');
        document.getElementById('saveBtn').classList.remove('hidden');
        document.getElementById('cancelEditBtn').classList.remove('hidden');
        document.getElementById('deleteBtn').classList.add('hidden');
    }

    /**
     * Cancel edit
     */
    cancelEdit() {
        if (this.isCreateMode) {
            // Exit create mode and go back to grid
            this.isCreateMode = false;
            this.isEditMode = false;
            this.switchTab('grid');
            return;
        }

        this.isEditMode = false;

        // Restore original data
        if (this.originalEntityData) {
            this.currentEntity.entity = this.originalEntityData;
            this.buildDetailForm(
                this.currentEntity.entity,
                this.currentEntity.entityTypeInfo,
                this.currentEntity.entitySetName
            );
        }

        this.originalEntityData = null;

        // Update action buttons
        document.getElementById('editBtn').classList.remove('hidden');
        document.getElementById('saveBtn').classList.add('hidden');
        document.getElementById('cancelEditBtn').classList.add('hidden');
        document.getElementById('deleteBtn').classList.remove('hidden');
    }

    /**
     * Save entity (create or update)
     */
    async saveEntity() {
        if (!this.isAuthenticated()) {
            alert('Please login to save entities');
            return;
        }

        this.showLoading(true);

        try {
            // Collect form data
            const entityData = this.collectFormData();

            if (this.isCreateMode) {
                // Create new entity
                const createdEntity = await this.odataService.createEntity(
                    this.currentEntitySet,
                    entityData
                );

                alert('Entity created successfully!');

                // Exit create mode
                this.isCreateMode = false;
                this.isEditMode = false;

                // Refresh grid
                await this.loadEntitySet(this.currentEntitySet);

                // Switch back to grid
                this.switchTab('grid');
            } else {
                // Update existing entity
                const keyValue = this.odataService.extractKeyValue(
                    this.currentEntity.entity,
                    this.currentEntity.entityTypeInfo
                );

                await this.odataService.updateEntity(
                    this.currentEntity.entitySetName,
                    keyValue,
                    entityData,
                    this.currentEntity.entityTypeInfo
                );

                alert('Entity updated successfully!');

                // Update current entity with new data
                this.currentEntity.entity = { ...this.currentEntity.entity, ...entityData };

                // Exit edit mode
                this.isEditMode = false;
                this.originalEntityData = null;

                // Rebuild form in read-only mode
                this.buildDetailForm(
                    this.currentEntity.entity,
                    this.currentEntity.entityTypeInfo,
                    this.currentEntity.entitySetName
                );

                // Refresh grid
                await this.loadEntitySet(this.currentEntitySet);
            }
        } catch (error) {
            this.showError('Failed to save entity', error);
        } finally {
            this.showLoading(false);
        }
    }

    /**
     * Collect form data from inputs
     */
    collectFormData() {
        const form = document.querySelector('#detailContainer .detail-form');
        const entityData = {};

        if (!form) return entityData;

        const formGroups = form.querySelectorAll('.form-group');
        formGroups.forEach(group => {
            const label = group.querySelector('label');
            if (!label) return;

            const propName = label.textContent.replace(' 🔑', '').replace(' *', '').trim();
            const input = group.querySelector('input, textarea, select');

            if (!input) return;

            // Skip readonly fields for key properties in update mode (not create mode)
            if (!this.isCreateMode && group.classList.contains('type-key')) {
                return;
            }

            let value;
            if (input.type === 'checkbox') {
                value = input.checked;
            } else if (input.type === 'number') {
                value = input.value ? parseFloat(input.value) : null;
            } else if (input.type === 'datetime-local') {
                value = input.value ? new Date(input.value).toISOString() : null;
            } else {
                value = input.value || null;
            }

            entityData[propName] = value;
        });

        return entityData;
    }

    /**
     * Delete entity
     */
    async deleteEntity() {
        if (!this.isAuthenticated()) {
            alert('Please login to delete entities');
            return;
        }

        const confirmDelete = confirm('Are you sure you want to delete this entity? This action cannot be undone.');
        if (!confirmDelete) {
            return;
        }

        this.showLoading(true);

        try {
            const keyValue = this.odataService.extractKeyValue(
                this.currentEntity.entity,
                this.currentEntity.entityTypeInfo
            );

            await this.odataService.deleteEntity(
                this.currentEntity.entitySetName,
                keyValue,
                this.currentEntity.entityTypeInfo
            );

            alert('Entity deleted successfully!');

            // Refresh grid
            await this.loadEntitySet(this.currentEntitySet);

            // Switch back to grid
            this.switchTab('grid');

            // Clear detail view
            this.clearDetailView();
        } catch (error) {
            this.showError('Failed to delete entity', error);
        } finally {
            this.showLoading(false);
        }
    }

    // ========== Complex Type Methods ==========

    /**
     * Show complex type modal
     */
    showComplexTypeModal(propertyName, complexTypeInfo, currentValue) {
        this.currentComplexTypeProperty = propertyName;
        this.currentComplexTypeData = currentValue || {};

        document.getElementById('complexTypeTitle').textContent = `Edit ${propertyName}`;

        const modalBody = document.getElementById('complexTypeBody');
        modalBody.innerHTML = '';

        // Create form for complex type properties
        complexTypeInfo.properties.forEach(prop => {
            const formGroup = this.createComplexTypeField(
                prop,
                this.currentComplexTypeData[prop.name]
            );
            modalBody.appendChild(formGroup);
        });

        document.getElementById('complexTypeModal').classList.remove('hidden');
    }

    /**
     * Create form field for complex type property
     */
    createComplexTypeField(property, value) {
        const formGroup = document.createElement('div');
        formGroup.className = 'form-group';

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
                checkbox.dataset.property = property.name;
                const checkboxLabel = document.createElement('span');
                checkboxLabel.textContent = value ? 'True' : 'False';
                input.appendChild(checkbox);
                input.appendChild(checkboxLabel);
                checkbox.addEventListener('change', () => {
                    checkboxLabel.textContent = checkbox.checked ? 'True' : 'False';
                });
                break;

            case 'number':
                input = document.createElement('input');
                input.type = 'number';
                input.value = value !== null && value !== undefined ? value : '';
                input.dataset.property = property.name;
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
                input.dataset.property = property.name;
                break;

            default:
                input = document.createElement('input');
                input.type = 'text';
                input.value = value !== null && value !== undefined ? value : '';
                input.dataset.property = property.name;
                break;
        }

        formGroup.appendChild(input);
        return formGroup;
    }

    /**
     * Hide complex type modal
     */
    hideComplexTypeModal() {
        document.getElementById('complexTypeModal').classList.add('hidden');
        this.currentComplexTypeProperty = null;
        this.currentComplexTypeData = null;
    }

    /**
     * Save complex type
     */
    saveComplexType() {
        const modalBody = document.getElementById('complexTypeBody');
        const complexTypeData = {};

        modalBody.querySelectorAll('.form-group').forEach(group => {
            const input = group.querySelector('input, textarea, select');
            if (!input) return;

            const propName = input.dataset.property;
            let value;

            if (input.type === 'checkbox') {
                value = input.checked;
            } else if (input.type === 'number') {
                value = input.value ? parseFloat(input.value) : null;
            } else if (input.type === 'datetime-local') {
                value = input.value ? new Date(input.value).toISOString() : null;
            } else {
                value = input.value || null;
            }

            complexTypeData[propName] = value;
        });

        // Update the main form's hidden input or display
        const mainForm = document.querySelector('#detailContainer .detail-form');
        if (mainForm) {
            const complexFieldGroups = mainForm.querySelectorAll('.form-group');
            complexFieldGroups.forEach(group => {
                const label = group.querySelector('label');
                if (label && label.textContent.trim() === this.currentComplexTypeProperty) {
                    // Update the complex type data display
                    const display = group.querySelector('.complex-type-field');
                    if (display) {
                        display.innerHTML = `<pre>${JSON.stringify(complexTypeData, null, 2)}</pre>`;
                    }

                    // Store data for saving
                    if (!this.currentEntity.entity) {
                        this.currentEntity.entity = {};
                    }
                    this.currentEntity.entity[this.currentComplexTypeProperty] = complexTypeData;
                }
            });
        }

        this.hideComplexTypeModal();
    }
}

// Initialize application when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.app = new ODataExplorer();

    // Set default service URL for testing
    const defaultUrl = 'https://services.odata.org/V4/TripPinServiceRW';
    document.getElementById('serviceUrl').value = defaultUrl;
});
