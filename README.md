# OData Service Explorer

A dynamic JavaScript application for exploring and visualizing OData services. This application automatically reads the `$metadata` endpoint of any OData service and creates an interactive user interface to browse entity sets, view data, and navigate relationships.

## Features

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
- **CORS Support**: Works with OData services that support CORS

## Files

- `index.html` - Main application structure
- `styles.css` - Styling and layout
- `odataService.js` - OData service interaction and metadata parsing
- `app.js` - Application logic and UI management

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

- Read-only: No create, update, or delete operations
- Basic type support: Complex types and enums require additional handling
- No authentication: Works only with public services or pre-authenticated sessions
- Requires CORS-enabled services

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

- Add export to CSV/Excel functionality
- Implement column sorting (click headers to sort)
- Add support for function imports and actions
- Create a favorites/bookmarks system for services
- Add dark mode theme
- Support for batch operations
- Add save/export filter configurations
- Implement advanced filter builder with AND/OR logic
- Add data visualization (charts/graphs)
- Support for editing and creating entities
