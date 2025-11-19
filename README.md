# OData Service Explorer

A dynamic JavaScript application for exploring and visualizing OData services. This application automatically reads the `$metadata` endpoint of any OData service and creates an interactive user interface to browse entity sets, view data, and navigate relationships.

## Features

### Data Exploration
- **Automatic Metadata Discovery**: Reads and parses the `$metadata` endpoint to understand the service structure
- **Entity Set Tree**: Displays all available entity sets in an organized tree view
- **Dynamic Grid View**: Shows entities in a data table with pagination support
- **Pagination**: Navigate through large datasets with configurable page sizes (10, 25, 50, 100 records per page)
- **Advanced Filtering**: Filter data by any property with multiple operators:
  - Equals, Not Equals
  - Greater Than, Greater or Equal, Less Than, Less or Equal
  - Contains, Starts With, Ends With
  - Multiple filters can be applied simultaneously
- **Detail Forms**: Automatically generates forms based on metadata with appropriate input types:
  - Text inputs for strings
  - Number inputs for numeric types
  - Datetime inputs for temporal data
  - Checkboxes for boolean values
  - Textareas for long text fields
- **Navigation Properties**: Supports viewing related entities through navigation properties
- **Recursive Exploration**: Allows drilling down through collections and related entities infinitely

### Authentication & Security
- **Keycloak/OpenID Connect Integration**: Full authentication support with Keycloak
- **JWT Token Management**: Automatic token refresh and session persistence
- **Configurable Auth**: Easy setup via UI for realm, client ID, and server URL
- **User Info Display**: Shows authenticated user information
- **Secure API Calls**: Automatically includes authentication headers in all requests

### CRUD Operations
- **Create Entities**: Create new entities with automatically generated forms
- **Edit Entities**: Edit existing entities with in-place form editing
- **Delete Entities**: Delete entities with confirmation dialogs
- **Save Operations**: Full support for POST (create) and PATCH (update) operations
- **Complex Type Support**: Edit complex/nested properties via modal dialogs
- **Validation**: Type-aware validation and input controls

### Additional Features
- **Complex Types**: Full support for parsing and editing complex type properties
- **Modal Dialogs**: Clean UI for authentication config and complex type editing
- **Responsive Design**: Works on desktop and mobile devices
- **CORS Support**: Works with OData services that support CORS

## Files

- `index.html` - Main application structure
- `styles.css` - Styling and layout
- `auth.js` - Keycloak/OpenID Connect authentication module
- `odataService.js` - OData service interaction, metadata parsing, and CRUD operations
- `app.js` - Application logic, UI management, and CRUD workflows

## Usage

### Getting Started

1. **Open the Application**: Open `index.html` in a modern web browser

2. **Connect to a Service**:
   - Enter the OData service URL in the connection panel
   - Example: `https://services.odata.org/V4/TripPinServiceRW`
   - Click "Connect" or press Enter

3. **Browse Entity Sets**:
   - The left panel will populate with available entity sets
   - Click on any entity set to view its data

4. **View Data**:
   - The grid view shows records with pagination controls
   - Use page size selector to choose how many records to display (10, 25, 50, or 100)
   - Navigate between pages using First, Previous, Next, Last buttons or click specific page numbers
   - Click any row to see detailed information

5. **Filter Data**:
   - Click the "🔍 Filters" button to show the filter panel
   - Select a property from the dropdown (automatically populated based on entity metadata)
   - Choose a filter operator (equals, contains, greater than, etc.)
   - Enter a filter value and click "Apply"
   - Multiple filters can be active simultaneously
   - Active filters are displayed as badges that can be individually removed
   - Click "Clear" to remove all filters

6. **Explore Details**:
   - The detail view shows all properties in an appropriate form format
   - Key fields are marked with 🔑
   - Required fields are marked with *

7. **Navigate Relationships**:
   - Click "View Collection" or "View Related Entity" buttons for navigation properties
   - Use the "Back" button to return to previous views

### Authentication Setup

1. **Configure Keycloak**:
   - Click the ⚙️ (settings) button in the header
   - Enter your Keycloak configuration:
     - **Realm**: Your Keycloak realm name
     - **Client ID**: Your client ID for the application
     - **Base URL** (optional): Leave empty to use the same domain, or enter your Keycloak server URL
   - Click "Save Configuration"

2. **Login**:
   - Click the "Login" button in the header
   - You'll be redirected to Keycloak for authentication
   - After successful login, you'll be redirected back with your username displayed
   - You can now perform CRUD operations

3. **Logout**:
   - Click the "Logout" button to end your session

### CRUD Operations

**Note**: Authentication is required for all CRUD operations (Create, Update, Delete)

#### Creating Entities

1. Select an entity set from the tree
2. Click the "➕ New" button in the grid header
3. Fill in the form with appropriate values:
   - Required fields are marked with *
   - Input types match the property types (text, number, datetime, etc.)
   - For complex types, click the "Edit" button to open a modal dialog
4. Click "💾 Save" to create the entity
5. Click "✖️ Cancel" to discard changes

#### Editing Entities

1. Click on any row in the grid to view the entity details
2. Click the "✏️ Edit" button in the detail view
3. Modify the values in the form:
   - Key properties cannot be modified
   - All other fields become editable
   - For complex types, click "Edit" to modify in a modal
4. Click "💾 Save" to update the entity
5. Click "✖️ Cancel" to discard changes and revert to original values

#### Deleting Entities

1. Click on any row in the grid to view the entity details
2. Click the "🗑️ Delete" button
3. Confirm the deletion in the dialog
4. The entity will be permanently deleted from the service

#### Working with Complex Types

Complex types (nested objects) are displayed as JSON in the detail view:

1. In edit/create mode, click the "Edit" button next to a complex type field
2. A modal dialog opens with individual fields for all complex type properties
3. Edit the values as needed
4. Click "OK" to save changes to the complex type
5. The updated complex type data will be included when saving the entity

### Example OData Services

Here are some publicly available OData services you can test with:

- **TripPin Service**: `https://services.odata.org/V4/TripPinServiceRW`
  - Sample travel service with People, Trips, Airlines, etc.

- **Northwind Service**: `https://services.odata.org/V4/Northwind/Northwind.svc`
  - Classic Northwind database with Customers, Orders, Products, etc.

- **OData Test Service**: `https://services.odata.org/V4/OData/OData.svc`
  - Simple test service with Products, Categories, etc.

### Supported Features

#### Metadata Parsing
- Entity Types with properties
- Entity Sets
- Navigation Properties (single and collection)
- Property Types (Edm.String, Edm.Int32, Edm.Boolean, etc.)
- Key Properties
- Nullable/Required properties

#### Data Types
The application automatically detects and creates appropriate inputs for:
- **String**: Text input or textarea (based on MaxLength)
- **Numbers**: Int16, Int32, Int64, Decimal, Double, Single → Number input
- **Boolean**: Checkbox
- **DateTime**: DateTimeOffset, Date, TimeOfDay → Datetime input
- **GUID**: Text input

#### Navigation
- Single navigation properties (1:1, N:1 relationships)
- Collection navigation properties (1:N, N:N relationships)
- Nested navigation through multiple levels
- Back navigation through breadcrumb trail

## Technical Details

### Architecture

The application uses a clean, modular architecture:

1. **ODataService Class** (`odataService.js`):
   - Handles all OData protocol operations
   - Parses XML metadata
   - Fetches entity sets and individual entities
   - Manages navigation property requests

2. **ODataExplorer Class** (`app.js`):
   - Manages UI state
   - Builds dynamic components (tree, grid, forms)
   - Handles user interactions
   - Maintains navigation history

### CORS Considerations

This application runs entirely in the browser and requires the OData service to support CORS (Cross-Origin Resource Sharing). If you encounter CORS errors:

1. **Use a CORS Proxy**: Services like `https://cors-anywhere.herokuapp.com/` can proxy requests
2. **Run a Local Server**: Use a development server with CORS enabled
3. **Browser Extensions**: Use a CORS extension (for development only)
4. **Deploy Backend Proxy**: Create a backend service that proxies OData requests

### Browser Compatibility

The application uses modern JavaScript features and is compatible with:
- Chrome/Edge 90+
- Firefox 88+
- Safari 14+

### Performance

- Configurable page sizes (10, 25, 50, 100 records per page) for optimal data loading
- Uses server-side pagination with OData `$top` and `$skip` parameters
- Applies filters on the server side using OData `$filter` expressions
- Uses `$count` to show total available records
- Lazy loads navigation properties only when requested
- Caches metadata after initial load

## Development

### Running Locally

The simplest way to run the application is to open `index.html` directly in a browser. However, for better development experience:

```bash
# Using Python 3
python -m http.server 8000

# Using Node.js http-server
npx http-server

# Using PHP
php -S localhost:8000
```

Then navigate to `http://localhost:8000`

### Extending the Application

#### Adding Edit Functionality

To make fields editable:

1. Remove `readonly` attribute in `createFormField()`
2. Add save button and implement PUT/PATCH requests
3. Handle validation based on metadata constraints

#### Adding Sorting

Modify the grid headers to add sorting functionality:

```javascript
const result = await this.odataService.fetchEntitySet(entitySetName, {
    top: this.pageSize,
    orderby: "PropertyName asc"
});
```

## Limitations

- **Enum Support**: Enumerations are treated as strings (no dropdown for enum values)
- **Batch Operations**: No support for OData batch requests
- **Function/Action Imports**: OData functions and actions are not yet supported
- **Media Entities**: Binary/media entity types not fully supported
- **CORS**: Requires CORS-enabled OData services for browser access
- **Authentication**: Currently supports only Keycloak/OpenID Connect (configurable)

## Security Considerations

- **Token Storage**: JWT tokens are stored in sessionStorage (cleared on browser close)
- **HTTPS Required**: Always use HTTPS for production deployments with authentication
- **CORS**: Ensure your OData service and Keycloak server have appropriate CORS settings
- **Client-Side App**: This is a client-side application - all data is processed in the browser
- **Public Clients**: Keycloak client should be configured as a "public" client (no client secret)

## Troubleshooting

### "Failed to fetch metadata"
- Check that the service URL is correct
- Ensure the service supports CORS
- Verify the service is accessible from your network

### "No entity sets found"
- The service might use a different metadata format
- Check browser console for XML parsing errors

### Data not displaying
- The entity set might be empty
- Check network tab for failed requests
- Verify the service is returning valid OData JSON

## License

This is a demonstration application. Feel free to use and modify for your needs.

## Contributing

Suggestions and improvements are welcome. Some ideas for enhancement:

- **Export Functionality**: Add export to CSV/Excel functionality
- **Column Sorting**: Implement column sorting (click headers to sort)
- **Batch Operations**: Support for OData batch requests
- **Function/Action Imports**: Add support for executing OData functions and actions
- **Enum Support**: Dropdown selectors for enumeration types
- **Favorites/Bookmarks**: System for saving frequently used services and queries
- **Dark Mode**: Add dark/light theme toggle
- **Advanced Filters**: Implement filter builder with AND/OR logic
- **Data Visualization**: Add charts and graphs for data analysis
- **Multi-Auth Support**: Add support for other OAuth providers (Azure AD, Auth0, etc.)
- **File Upload**: Support for media entities and file uploads
- **Offline Mode**: Cache data for offline browsing
- **Query Builder**: Visual query builder for complex OData queries
