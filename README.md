# OData Metadata Endpoint Reader

A comprehensive web-based tool for exploring, querying, and interacting with OData services. This application parses OData metadata endpoints and provides an intuitive interface for working with entities, executing functions/actions, and performing batch operations.

## Features

### 1. Dropdown Selectors for Enumeration Types
- Automatically detects enumeration types in entity properties
- Displays user-friendly dropdown menus instead of text inputs for enum fields
- Shows enum values with their descriptions in filters, function parameters, and action parameters
- Provides visual badges to identify enum properties in entity type listings

### 2. Batch Operations Support
- Create multiple OData requests (GET, POST, PATCH, DELETE) in a single batch
- Dynamically add and remove batch requests through the UI
- Supports both query operations and change sets
- Execute all batch requests with a single HTTP call
- View formatted batch responses

### 3. OData Functions and Actions
- **Functions**: Execute OData functions with parameter inputs
  - Automatically generates input forms based on function metadata
  - Supports both simple types and enumeration parameters
  - Displays return type information
- **Actions**: Execute OData actions with POST requests
  - Parameter input forms with enum dropdown support
  - JSON payload generation
  - Result display and management

### 4. Additional Features
- **Metadata Parsing**: Automatically parses OData $metadata XML to extract:
  - Entity types and their properties
  - Entity sets
  - Enumeration types
  - Functions and actions
  - Navigation properties
- **Entity Explorer**: Browse entity sets and view property details
- **Query Builder**: Construct OData queries with:
  - Property filtering (with enum dropdown support)
  - Top/limit clause
  - Smart input types based on property types
- **Results Management**:
  - View formatted JSON results
  - Copy results to clipboard
  - Download results as JSON files

## Usage

### Getting Started

1. Open `index.html` in a modern web browser
2. Enter an OData service URL (e.g., `https://services.odata.org/V4/TripPinService`)
3. Click "Load Metadata" to parse the service metadata

### Exploring Entities

1. Select an entity set from the dropdown
2. View entity properties with type information and attributes
3. Build queries using the query builder:
   - Select a property to filter by
   - For enum properties, use the dropdown selector
   - For other types, enter a value in the text input
   - Set the top/limit value
4. Click "Execute Query" to run the query

### Using Functions and Actions

#### Functions
1. Navigate to the "Functions & Actions" panel
2. Select the "Functions" tab
3. Choose a function from the dropdown
4. Fill in the required parameters (enum parameters will show dropdowns)
5. Click "Execute Function"

#### Actions
1. Navigate to the "Functions & Actions" panel
2. Select the "Actions" tab
3. Choose an action from the dropdown
4. Fill in the required parameters (enum parameters will show dropdowns)
5. Click "Execute Action"

### Batch Operations

1. Navigate to the "Batch Operations" panel
2. Click "Add Request" to add a new batch request
3. For each request:
   - Select the HTTP method (GET, POST, PATCH, DELETE)
   - Choose an entity set
   - Optionally add a path (e.g., `(1)` for a specific entity)
   - For POST/PATCH, add a JSON body
4. Click "Execute Batch" to run all requests
5. Use "Clear All" to remove all batch requests

## File Structure

```
.
├── index.html      # Main HTML structure and UI components
├── styles.css      # Responsive styling and animations
├── app.js          # Core application logic and OData client
└── README.md       # This file
```

## Technical Details

### Supported OData Features

- OData V4 metadata parsing
- Entity types with properties and keys
- Enumeration types with member values
- Navigation properties
- Unbound functions and actions
- Batch requests ($batch endpoint)
- Query options ($filter, $top)

### Browser Compatibility

- Modern browsers with ES6+ support
- Fetch API support required
- No external dependencies

### CORS Considerations

When accessing OData services from a browser, ensure the target service supports CORS or use a CORS proxy for testing purposes.

## Example Services

Try these public OData services:

- TripPin Service: `https://services.odata.org/V4/TripPinService`
- Northwind Service: `https://services.odata.org/V4/Northwind/Northwind.svc`
- OData Demo Service: `https://services.odata.org/V4/OData/OData.svc`

## Implementation Highlights

### Enumeration Type Detection
The application detects enumeration types by:
1. Parsing `<EnumType>` elements from metadata
2. Storing enum members with their values
3. Checking property types against the enum type registry
4. Dynamically generating dropdown selectors when enum types are detected

### Batch Request Format
Batch requests use the multipart/mixed format with:
- Unique boundary identifiers
- Proper Content-Type headers
- Changeset grouping for non-GET operations
- Content-ID tracking for request correlation

### Function vs Action Execution
- **Functions**: Use GET requests with parameters in the URL
- **Actions**: Use POST requests with parameters in the JSON body

## License

See LICENSE file for details.
