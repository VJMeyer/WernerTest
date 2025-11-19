// OData Metadata Endpoint Reader Application
class ODataMetadataReader {
    constructor() {
        this.serviceUrl = '';
        this.metadata = null;
        this.entityTypes = new Map();
        this.entitySets = new Map();
        this.enumTypes = new Map();
        this.functions = new Map();
        this.actions = new Map();
        this.batchRequests = [];

        this.init();
    }

    init() {
        this.setupEventListeners();
    }

    setupEventListeners() {
        // Load metadata
        document.getElementById('loadMetadata').addEventListener('click', () => this.loadMetadata());

        // Entity set selection
        document.getElementById('entitySetSelect').addEventListener('change', (e) => this.onEntitySetChange(e.target.value));

        // Filter property selection
        document.getElementById('filterProperty').addEventListener('change', (e) => this.onFilterPropertyChange(e.target.value));

        // Execute query
        document.getElementById('executeQuery').addEventListener('click', () => this.executeQuery());

        // Function/Action tabs
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.addEventListener('click', (e) => this.switchTab(e.target.dataset.tab));
        });

        // Function selection
        document.getElementById('functionSelect').addEventListener('change', (e) => this.onFunctionChange(e.target.value));

        // Action selection
        document.getElementById('actionSelect').addEventListener('change', (e) => this.onActionChange(e.target.value));

        // Execute function/action
        document.getElementById('executeFunction').addEventListener('click', () => this.executeFunction());
        document.getElementById('executeAction').addEventListener('click', () => this.executeAction());

        // Batch operations
        document.getElementById('addBatchRequest').addEventListener('click', () => this.addBatchRequest());
        document.getElementById('executeBatch').addEventListener('click', () => this.executeBatch());
        document.getElementById('clearBatch').addEventListener('click', () => this.clearBatch());

        // Results actions
        document.getElementById('copyResults').addEventListener('click', () => this.copyResults());
        document.getElementById('downloadResults').addEventListener('click', () => this.downloadResults());
    }

    async loadMetadata() {
        const urlInput = document.getElementById('serviceUrl');
        this.serviceUrl = urlInput.value.trim().replace(/\/$/, '');

        if (!this.serviceUrl) {
            this.showStatus('Please enter a service URL', 'error');
            return;
        }

        this.showStatus('Loading metadata...', 'info');

        try {
            const metadataUrl = `${this.serviceUrl}/$metadata`;
            const response = await fetch(metadataUrl, {
                headers: {
                    'Accept': 'application/xml'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const xmlText = await response.text();
            const parser = new DOMParser();
            this.metadata = parser.parseFromString(xmlText, 'text/xml');

            // Check for parsing errors
            const parserError = this.metadata.querySelector('parsererror');
            if (parserError) {
                throw new Error('Failed to parse metadata XML');
            }

            this.parseMetadata();
            this.displayMetadataInfo();
            this.populateEntitySets();
            this.populateFunctionsAndActions();

            this.showStatus('Metadata loaded successfully!', 'success');

            // Show panels
            document.getElementById('metadataPanel').style.display = 'block';
            document.getElementById('entityPanel').style.display = 'block';
            document.getElementById('functionsPanel').style.display = 'block';
            document.getElementById('batchPanel').style.display = 'block';

        } catch (error) {
            this.showStatus(`Error loading metadata: ${error.message}`, 'error');
            console.error('Metadata loading error:', error);
        }
    }

    parseMetadata() {
        // Parse entity types
        const entityTypeElements = this.metadata.querySelectorAll('EntityType');
        entityTypeElements.forEach(element => {
            const name = element.getAttribute('Name');
            const properties = [];
            const navigationProperties = [];

            element.querySelectorAll('Property').forEach(prop => {
                properties.push({
                    name: prop.getAttribute('Name'),
                    type: prop.getAttribute('Type'),
                    nullable: prop.getAttribute('Nullable') !== 'false',
                    maxLength: prop.getAttribute('MaxLength'),
                    precision: prop.getAttribute('Precision'),
                    scale: prop.getAttribute('Scale')
                });
            });

            element.querySelectorAll('Key PropertyRef').forEach(key => {
                const keyName = key.getAttribute('Name');
                const prop = properties.find(p => p.name === keyName);
                if (prop) prop.isKey = true;
            });

            element.querySelectorAll('NavigationProperty').forEach(nav => {
                navigationProperties.push({
                    name: nav.getAttribute('Name'),
                    type: nav.getAttribute('Type')
                });
            });

            this.entityTypes.set(name, {
                name,
                properties,
                navigationProperties
            });
        });

        // Parse enum types
        const enumTypeElements = this.metadata.querySelectorAll('EnumType');
        enumTypeElements.forEach(element => {
            const name = element.getAttribute('Name');
            const members = [];

            element.querySelectorAll('Member').forEach(member => {
                members.push({
                    name: member.getAttribute('Name'),
                    value: member.getAttribute('Value')
                });
            });

            this.enumTypes.set(name, { name, members });
        });

        // Parse entity sets
        const entitySetElements = this.metadata.querySelectorAll('EntitySet');
        entitySetElements.forEach(element => {
            const name = element.getAttribute('Name');
            const entityType = element.getAttribute('EntityType');
            const typeName = entityType.split('.').pop();

            this.entitySets.set(name, {
                name,
                entityType: typeName
            });
        });

        // Parse functions
        const functionElements = this.metadata.querySelectorAll('Function');
        functionElements.forEach(element => {
            const name = element.getAttribute('Name');
            const isBound = element.getAttribute('IsBound') === 'true';
            const parameters = [];
            let returnType = null;

            element.querySelectorAll('Parameter').forEach(param => {
                parameters.push({
                    name: param.getAttribute('Name'),
                    type: param.getAttribute('Type'),
                    nullable: param.getAttribute('Nullable') !== 'false'
                });
            });

            const returnTypeElement = element.querySelector('ReturnType');
            if (returnTypeElement) {
                returnType = returnTypeElement.getAttribute('Type');
            }

            this.functions.set(name, {
                name,
                isBound,
                parameters,
                returnType
            });
        });

        // Parse actions
        const actionElements = this.metadata.querySelectorAll('Action');
        actionElements.forEach(element => {
            const name = element.getAttribute('Name');
            const isBound = element.getAttribute('IsBound') === 'true';
            const parameters = [];
            let returnType = null;

            element.querySelectorAll('Parameter').forEach(param => {
                parameters.push({
                    name: param.getAttribute('Name'),
                    type: param.getAttribute('Type'),
                    nullable: param.getAttribute('Nullable') !== 'false'
                });
            });

            const returnTypeElement = element.querySelector('ReturnType');
            if (returnTypeElement) {
                returnType = returnTypeElement.getAttribute('Type');
            }

            this.actions.set(name, {
                name,
                isBound,
                parameters,
                returnType
            });
        });
    }

    displayMetadataInfo() {
        const schemas = this.metadata.querySelectorAll('Schema');
        const namespaces = Array.from(schemas).map(s => s.getAttribute('Namespace')).join(', ');

        document.getElementById('serviceVersion').textContent = '4.0';
        document.getElementById('namespaces').textContent = namespaces || 'Default';
        document.getElementById('entitySetCount').textContent = this.entitySets.size;
    }

    populateEntitySets() {
        const select = document.getElementById('entitySetSelect');
        select.innerHTML = '<option value="">-- Select an Entity Set --</option>';

        this.entitySets.forEach((entitySet, name) => {
            const option = document.createElement('option');
            option.value = name;
            option.textContent = `${name} (${entitySet.entityType})`;
            select.appendChild(option);
        });
    }

    onEntitySetChange(entitySetName) {
        if (!entitySetName) {
            document.getElementById('entityDetails').style.display = 'none';
            return;
        }

        const entitySet = this.entitySets.get(entitySetName);
        const entityType = this.entityTypes.get(entitySet.entityType);

        if (!entityType) {
            this.showStatus('Entity type not found', 'error');
            return;
        }

        this.displayEntityProperties(entityType);
        this.populateFilterProperties(entityType);
        document.getElementById('entityDetails').style.display = 'block';
    }

    displayEntityProperties(entityType) {
        const container = document.getElementById('propertiesList');

        let html = '<table class="properties-table"><thead><tr><th>Property</th><th>Type</th><th>Attributes</th></tr></thead><tbody>';

        entityType.properties.forEach(prop => {
            const typeName = prop.type.split('.').pop();
            const isEnum = this.enumTypes.has(typeName);

            let badges = '';
            if (prop.isKey) badges += '<span class="property-badge badge-key">KEY</span>';
            if (prop.nullable) badges += '<span class="property-badge badge-nullable">Nullable</span>';
            if (isEnum) badges += '<span class="property-badge badge-enum">ENUM</span>';

            html += `<tr>
                <td><strong>${prop.name}</strong></td>
                <td>${typeName}</td>
                <td>${badges}</td>
            </tr>`;
        });

        html += '</tbody></table>';
        container.innerHTML = html;
    }

    populateFilterProperties(entityType) {
        const select = document.getElementById('filterProperty');
        select.innerHTML = '<option value="">-- Select Property --</option>';

        entityType.properties.forEach(prop => {
            const option = document.createElement('option');
            option.value = prop.name;
            option.dataset.type = prop.type;
            option.textContent = prop.name;
            select.appendChild(option);
        });
    }

    onFilterPropertyChange(propertyName) {
        const select = document.getElementById('filterProperty');
        const valueGroup = document.getElementById('filterValueGroup');
        const textInput = document.getElementById('filterValue');
        const enumSelect = document.getElementById('filterEnum');

        if (!propertyName) {
            valueGroup.style.display = 'none';
            return;
        }

        const selectedOption = select.options[select.selectedIndex];
        const propertyType = selectedOption.dataset.type;
        const typeName = propertyType.split('.').pop();
        const enumType = this.enumTypes.get(typeName);

        valueGroup.style.display = 'block';

        if (enumType) {
            // Show dropdown for enum types
            textInput.style.display = 'none';
            enumSelect.style.display = 'block';

            enumSelect.innerHTML = '<option value="">-- Select Value --</option>';
            enumType.members.forEach(member => {
                const option = document.createElement('option');
                option.value = member.name;
                option.textContent = `${member.name} (${member.value})`;
                enumSelect.appendChild(option);
            });
        } else {
            // Show text input for other types
            textInput.style.display = 'block';
            enumSelect.style.display = 'none';
            textInput.value = '';

            // Set appropriate input type
            if (propertyType.includes('Int') || propertyType.includes('Decimal') || propertyType.includes('Double')) {
                textInput.type = 'number';
            } else {
                textInput.type = 'text';
            }
        }
    }

    async executeQuery() {
        const entitySetName = document.getElementById('entitySetSelect').value;
        const filterProperty = document.getElementById('filterProperty').value;
        const topCount = document.getElementById('topCount').value;

        if (!entitySetName) {
            this.showStatus('Please select an entity set', 'error');
            return;
        }

        let queryUrl = `${this.serviceUrl}/${entitySetName}`;
        const queryParams = [];

        if (filterProperty) {
            const enumSelect = document.getElementById('filterEnum');
            const textInput = document.getElementById('filterValue');

            let filterValue;
            if (enumSelect.style.display !== 'none') {
                filterValue = enumSelect.value;
                if (filterValue) {
                    queryParams.push(`$filter=${filterProperty} eq '${filterValue}'`);
                }
            } else {
                filterValue = textInput.value;
                if (filterValue) {
                    const selectedOption = document.getElementById('filterProperty').options[document.getElementById('filterProperty').selectedIndex];
                    const propertyType = selectedOption.dataset.type;

                    if (propertyType.includes('String')) {
                        queryParams.push(`$filter=${filterProperty} eq '${filterValue}'`);
                    } else {
                        queryParams.push(`$filter=${filterProperty} eq ${filterValue}`);
                    }
                }
            }
        }

        if (topCount) {
            queryParams.push(`$top=${topCount}`);
        }

        if (queryParams.length > 0) {
            queryUrl += '?' + queryParams.join('&');
        }

        this.showStatus('Executing query...', 'info');

        try {
            const response = await fetch(queryUrl, {
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();
            this.displayResults(data);
            this.showStatus('Query executed successfully!', 'success');

        } catch (error) {
            this.showStatus(`Error executing query: ${error.message}`, 'error');
            console.error('Query execution error:', error);
        }
    }

    populateFunctionsAndActions() {
        // Populate functions
        const functionSelect = document.getElementById('functionSelect');
        functionSelect.innerHTML = '<option value="">-- Select a Function --</option>';

        this.functions.forEach((func, name) => {
            if (!func.isBound) { // Only show unbound functions for now
                const option = document.createElement('option');
                option.value = name;
                option.textContent = name;
                functionSelect.appendChild(option);
            }
        });

        // Populate actions
        const actionSelect = document.getElementById('actionSelect');
        actionSelect.innerHTML = '<option value="">-- Select an Action --</option>';

        this.actions.forEach((action, name) => {
            if (!action.isBound) { // Only show unbound actions for now
                const option = document.createElement('option');
                option.value = name;
                option.textContent = name;
                actionSelect.appendChild(option);
            }
        });
    }

    onFunctionChange(functionName) {
        const details = document.getElementById('functionDetails');
        const paramsContainer = document.getElementById('functionParams');

        if (!functionName) {
            details.style.display = 'none';
            return;
        }

        const func = this.functions.get(functionName);
        paramsContainer.innerHTML = '';

        func.parameters.forEach(param => {
            const paramDiv = document.createElement('div');
            paramDiv.className = 'param-input';

            const typeName = param.type.split('.').pop();
            const isEnum = this.enumTypes.has(typeName);

            let inputHtml = `<label>${param.name} (${typeName})${param.nullable ? '' : ' *'}</label>`;

            if (isEnum) {
                const enumType = this.enumTypes.get(typeName);
                inputHtml += `<select class="form-control" data-param="${param.name}">`;
                inputHtml += '<option value="">-- Select --</option>';
                enumType.members.forEach(member => {
                    inputHtml += `<option value="${member.name}">${member.name}</option>`;
                });
                inputHtml += '</select>';
            } else {
                inputHtml += `<input type="text" class="form-control" data-param="${param.name}" placeholder="Enter ${param.name}">`;
            }

            paramDiv.innerHTML = inputHtml;
            paramsContainer.appendChild(paramDiv);
        });

        details.style.display = 'block';
    }

    onActionChange(actionName) {
        const details = document.getElementById('actionDetails');
        const paramsContainer = document.getElementById('actionParams');

        if (!actionName) {
            details.style.display = 'none';
            return;
        }

        const action = this.actions.get(actionName);
        paramsContainer.innerHTML = '';

        action.parameters.forEach(param => {
            const paramDiv = document.createElement('div');
            paramDiv.className = 'param-input';

            const typeName = param.type.split('.').pop();
            const isEnum = this.enumTypes.has(typeName);

            let inputHtml = `<label>${param.name} (${typeName})${param.nullable ? '' : ' *'}</label>`;

            if (isEnum) {
                const enumType = this.enumTypes.get(typeName);
                inputHtml += `<select class="form-control" data-param="${param.name}">`;
                inputHtml += '<option value="">-- Select --</option>';
                enumType.members.forEach(member => {
                    inputHtml += `<option value="${member.name}">${member.name}</option>`;
                });
                inputHtml += '</select>';
            } else {
                inputHtml += `<input type="text" class="form-control" data-param="${param.name}" placeholder="Enter ${param.name}">`;
            }

            paramDiv.innerHTML = inputHtml;
            paramsContainer.appendChild(paramDiv);
        });

        details.style.display = 'block';
    }

    async executeFunction() {
        const functionName = document.getElementById('functionSelect').value;

        if (!functionName) {
            this.showStatus('Please select a function', 'error');
            return;
        }

        const func = this.functions.get(functionName);
        const params = {};

        // Collect parameter values
        const paramInputs = document.querySelectorAll('#functionParams [data-param]');
        paramInputs.forEach(input => {
            const paramName = input.dataset.param;
            const value = input.value;
            if (value) {
                params[paramName] = value;
            }
        });

        // Build function URL with parameters
        let functionUrl = `${this.serviceUrl}/${functionName}`;

        if (Object.keys(params).length > 0) {
            const paramString = Object.entries(params)
                .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
                .join(',');
            functionUrl += `(${paramString})`;
        } else {
            functionUrl += '()';
        }

        this.showStatus('Executing function...', 'info');

        try {
            const response = await fetch(functionUrl, {
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();
            this.displayResults(data);
            this.showStatus('Function executed successfully!', 'success');

        } catch (error) {
            this.showStatus(`Error executing function: ${error.message}`, 'error');
            console.error('Function execution error:', error);
        }
    }

    async executeAction() {
        const actionName = document.getElementById('actionSelect').value;

        if (!actionName) {
            this.showStatus('Please select an action', 'error');
            return;
        }

        const action = this.actions.get(actionName);
        const params = {};

        // Collect parameter values
        const paramInputs = document.querySelectorAll('#actionParams [data-param]');
        paramInputs.forEach(input => {
            const paramName = input.dataset.param;
            const value = input.value;
            if (value) {
                params[paramName] = value;
            }
        });

        const actionUrl = `${this.serviceUrl}/${actionName}`;

        this.showStatus('Executing action...', 'info');

        try {
            const response = await fetch(actionUrl, {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(params)
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();
            this.displayResults(data);
            this.showStatus('Action executed successfully!', 'success');

        } catch (error) {
            this.showStatus(`Error executing action: ${error.message}`, 'error');
            console.error('Action execution error:', error);
        }
    }

    addBatchRequest() {
        const requestId = this.batchRequests.length + 1;
        const container = document.getElementById('batchRequests');

        const requestDiv = document.createElement('div');
        requestDiv.className = 'batch-request-item';
        requestDiv.dataset.requestId = requestId;

        requestDiv.innerHTML = `
            <button class="remove-btn" onclick="odataReader.removeBatchRequest(${requestId})">Remove</button>
            <div class="form-group">
                <label>Method:</label>
                <select class="form-control batch-method">
                    <option value="GET">GET</option>
                    <option value="POST">POST</option>
                    <option value="PATCH">PATCH</option>
                    <option value="DELETE">DELETE</option>
                </select>
            </div>
            <div class="form-group">
                <label>Entity Set:</label>
                <select class="form-control batch-entity-set">
                    <option value="">-- Select Entity Set --</option>
                    ${Array.from(this.entitySets.keys()).map(name =>
                        `<option value="${name}">${name}</option>`
                    ).join('')}
                </select>
            </div>
            <div class="form-group">
                <label>Additional Path (optional):</label>
                <input type="text" class="form-control batch-path" placeholder="e.g., (1) or (ID=123)">
            </div>
            <div class="form-group">
                <label>Body (for POST/PATCH):</label>
                <textarea class="form-control batch-body" rows="3" placeholder='{"Property": "Value"}'></textarea>
            </div>
        `;

        container.appendChild(requestDiv);
        this.batchRequests.push({ id: requestId });
    }

    removeBatchRequest(requestId) {
        const requestDiv = document.querySelector(`[data-request-id="${requestId}"]`);
        if (requestDiv) {
            requestDiv.remove();
        }
        this.batchRequests = this.batchRequests.filter(r => r.id !== requestId);
    }

    async executeBatch() {
        const requestDivs = document.querySelectorAll('.batch-request-item');

        if (requestDivs.length === 0) {
            this.showStatus('Please add at least one batch request', 'error');
            return;
        }

        const boundary = `batch_${Date.now()}`;
        const changesetBoundary = `changeset_${Date.now()}`;

        let batchBody = '';
        const requests = [];

        requestDivs.forEach((div, index) => {
            const method = div.querySelector('.batch-method').value;
            const entitySet = div.querySelector('.batch-entity-set').value;
            const path = div.querySelector('.batch-path').value;
            const body = div.querySelector('.batch-body').value;

            if (!entitySet) return;

            let url = `${entitySet}${path}`;

            requests.push({
                method,
                url,
                body: body ? body : null
            });
        });

        if (requests.length === 0) {
            this.showStatus('No valid batch requests to execute', 'error');
            return;
        }

        // Build batch request body
        requests.forEach((req, index) => {
            if (req.method === 'GET') {
                batchBody += `--${boundary}\r\n`;
                batchBody += `Content-Type: application/http\r\n`;
                batchBody += `Content-Transfer-Encoding: binary\r\n\r\n`;
                batchBody += `GET ${req.url} HTTP/1.1\r\n`;
                batchBody += `Accept: application/json\r\n\r\n`;
            } else {
                if (index === 0 || requests[index - 1].method === 'GET') {
                    batchBody += `--${boundary}\r\n`;
                    batchBody += `Content-Type: multipart/mixed; boundary=${changesetBoundary}\r\n\r\n`;
                }

                batchBody += `--${changesetBoundary}\r\n`;
                batchBody += `Content-Type: application/http\r\n`;
                batchBody += `Content-Transfer-Encoding: binary\r\n`;
                batchBody += `Content-ID: ${index + 1}\r\n\r\n`;
                batchBody += `${req.method} ${req.url} HTTP/1.1\r\n`;
                batchBody += `Content-Type: application/json\r\n\r\n`;
                if (req.body) {
                    batchBody += `${req.body}\r\n`;
                }

                if (index === requests.length - 1 || requests[index + 1].method === 'GET') {
                    batchBody += `--${changesetBoundary}--\r\n`;
                }
            }
        });

        batchBody += `--${boundary}--\r\n`;

        this.showStatus('Executing batch request...', 'info');

        try {
            const response = await fetch(`${this.serviceUrl}/$batch`, {
                method: 'POST',
                headers: {
                    'Content-Type': `multipart/mixed; boundary=${boundary}`,
                    'Accept': 'multipart/mixed'
                },
                body: batchBody
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const responseText = await response.text();
            this.displayResults({ batchResponse: responseText });
            this.showStatus('Batch request executed successfully!', 'success');

        } catch (error) {
            this.showStatus(`Error executing batch: ${error.message}`, 'error');
            console.error('Batch execution error:', error);
        }
    }

    clearBatch() {
        document.getElementById('batchRequests').innerHTML = '';
        this.batchRequests = [];
    }

    switchTab(tabName) {
        // Update tab buttons
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.remove('active');
            if (btn.dataset.tab === tabName) {
                btn.classList.add('active');
            }
        });

        // Update tab content
        document.querySelectorAll('.tab-content').forEach(content => {
            content.style.display = 'none';
        });

        if (tabName === 'functions') {
            document.getElementById('functionsTab').style.display = 'block';
        } else if (tabName === 'actions') {
            document.getElementById('actionsTab').style.display = 'block';
        }
    }

    displayResults(data) {
        const panel = document.getElementById('resultsPanel');
        const content = document.getElementById('resultsContent');

        content.textContent = JSON.stringify(data, null, 2);
        panel.style.display = 'block';

        // Scroll to results
        panel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    copyResults() {
        const content = document.getElementById('resultsContent').textContent;
        navigator.clipboard.writeText(content).then(() => {
            this.showStatus('Results copied to clipboard!', 'success');
        }).catch(err => {
            this.showStatus('Failed to copy results', 'error');
        });
    }

    downloadResults() {
        const content = document.getElementById('resultsContent').textContent;
        const blob = new Blob([content], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `odata-results-${Date.now()}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }

    showStatus(message, type) {
        const statusElement = document.getElementById('connectionStatus');
        statusElement.textContent = message;
        statusElement.className = `status-message ${type}`;
    }
}

// Initialize the application
const odataReader = new ODataMetadataReader();
