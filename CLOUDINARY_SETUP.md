# Cloudinary Setup Guide

## How to Get Cloudinary API Credentials

### Step 1: Create a Free Cloudinary Account
1. Go to https://cloudinary.com/users/register/free
2. Sign up with your email
3. Verify your email address

### Step 2: Get Your API Credentials
1. After login, go to your Dashboard: https://console.cloudinary.com/
2. You'll see your credentials in the "Product Environment Credentials" section:
   - Cloud Name
   - API Key
   - API Secret

### Step 3: Update application.properties
Replace the values in `src/main/resources/application.properties`:

```properties
# Cloudinary Configuration
cloudinary.cloud-name=your_actual_cloud_name
cloudinary.api-key=your_actual_api_key
cloudinary.api-secret=your_actual_api_secret
```

**IMPORTANT:** 
- Keep your API Secret secure and never commit it to version control
- Consider using environment variables in production

### Step 4: Test the Upload
After updating the credentials and restarting your application, test the upload endpoint:

**API Endpoint:** `POST http://localhost:8081/api/produk/upload-image`

**Request (multipart/form-data):**
```
file: [select your image]
```

**Response:**
```json
{
  "success": true,
  "message": "Gambar berhasil diupload ke Cloudinary",
  "imageUrl": "https://res.cloudinary.com/your-cloud-name/image/upload/v1234567890/fajar-gold/products/uuid.jpg",
  "publicId": "fajar-gold/products/uuid"
}
```

### Step 5: Create Product with Cloudinary URL
**IMPORTANT:** Use the `imageUrl` from Step 4 response when creating a product!

**API Endpoint:** `POST http://localhost:8081/api/produk`

**Request (application/json):**
```json
{
  "nama": "Cincin Emas 24K",
  "gambar": "https://res.cloudinary.com/your-cloud-name/image/upload/v1234567890/fajar-gold/products/uuid.jpg",
  "harga": 5000000,
  "stock": 10,
  "karatEmas": 24
}
```

**Important Notes:**
- The `gambar` field MUST contain a full URL (starting with http:// or https://)
- Do NOT use filenames like "image.jpg" - the system will reject it
- Always upload the image first using `/api/produk/upload-image`, then use the returned `imageUrl`

### Workflow Summary:
1. **Upload Image** → Get Cloudinary URL
2. **Create Product** → Use the Cloudinary URL from step 1
3. Image is now accessible from anywhere! 🌍
