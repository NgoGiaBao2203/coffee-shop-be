# Supabase Storage Integration - Implementation Summary

## Overview
The Coffee Shop API has been successfully configured to store drink images in Supabase Storage (S3-compatible) instead of the local file system. This implementation provides cloud-based image storage with automatic image lifecycle management (delete old images when updating).

## Changes Made

### 1. **Dependencies Added** ([pom.xml](pom.xml))
- **AWS SDK v2 BOM**: Added Bill of Materials for dependency management
- **AWS S3 SDK**: `software.amazon.awssdk:s3` for S3-compatible storage operations

### 2. **Configuration** 
#### [application-dev.yaml](src/main/resources/application-dev.yaml) (Development)
```yaml
storage:
  supabase:
    enabled: true
    bucket-name: ${SUPABASE_BUCKET_NAME:drinks}
    region: ${SUPABASE_REGION:ap-south-1}
    endpoint: ${SUPABASE_ENDPOINT:https://aws-0-ap-south-1.pooler.supabase.com}
    access-key: ${SUPABASE_ACCESS_KEY:}
    secret-key: ${SUPABASE_SECRET_KEY:}
```

#### [application-prod.yaml](src/main/resources/application-prod.yaml) (Production)
- Same Supabase configuration for production use

### 3. **New Configuration Class**
[SupabaseStorageProperties.java](src/main/java/coffee/api/config/SupabaseStorageProperties.java)
- Binds YAML configuration to Java properties
- Managed as Spring Bean for dependency injection
- Properties:
  - `enabled`: Toggle S3 storage
  - `bucketName`: Supabase bucket name
  - `region`: AWS region
  - `endpoint`: Supabase endpoint URL
  - `accessKey`: S3 access key ID
  - `secretKey`: S3 secret access key

### 4. **Updated File Storage Service**
[FileStorageServiceImpl.java](src/main/java/coffee/api/services/services_implement/common/FileStorageServiceImpl.java)

**Key Methods:**
- `storeDrinkImage(MultipartFile)`: 
  - Uploads image to Supabase S3 storage
  - Returns public URL for database storage
  - URL format: `https://endpoint/storage/v1/object/public/{bucket}/{filename}`
  
- `deleteDrinkImage(String imageUrl)`:
  - Extracts file key from public URL
  - Deletes old image when replacing with new one
  - Throws `InvalidRequestException` on deletion failure

**Internal Methods:**
- `createS3Client()`: Builds AWS S3 client with Supabase credentials
- `extractFileKeyFromUrl(String)`: Parses public URL to extract S3 object key
- `extractAndValidateExtension(String)`: Validates file extension (.jpg, .png only)
- `extractBaseFileName(String)`: Extracts filename without extension

### 5. **Updated Tests**
[FileStorageServiceImplTest.java](src/test/java/coffee/api/services/services_implement/common/FileStorageServiceImplTest.java)
- 20 test cases covering:
  - Null and empty file handling
  - File extension validation
  - URL extraction and parsing
  - Exception handling
  - Base filename extraction

All tests pass successfully ✅

## Image Upload/Update Workflow

### When Editing a Drink with New Image:
1. User calls `PUT /drink/edit` with new image file
2. `EditDrinksServiceImpl.process()` is invoked
3. **New image is uploaded**:
   - `FileStorageServiceImpl.storeDrinkImage()` uploads to Supabase
   - Returns public URL
4. **Old image is deleted** (if exists):
   - Extracts old image URL from database
   - `FileStorageServiceImpl.deleteDrinkImage()` removes from Supabase
5. Database is updated with new image URL

### When Editing a Drink without New Image:
1. User calls `PUT /drink/edit` without image
2. `EditDrinksServiceImpl.process()` skips file operations
3. Old image remains unchanged in Supabase
4. Database maintains existing image URL

## Environment Configuration

### Development Setup
```bash
export SUPABASE_BUCKET_NAME=drinks
export SUPABASE_REGION=ap-south-1
export SUPABASE_ENDPOINT=https://aws-0-ap-south-1.pooler.supabase.com
export SUPABASE_ACCESS_KEY=<your-access-key>
export SUPABASE_SECRET_KEY=<your-secret-key>
```

### Production Setup
Set the same environment variables in your production environment/container.

## Testing Build Results
```
Tests run: 216, Failures: 0, Errors: 0
✅ All tests passed successfully
```

## File Storage Feature Comparison

| Feature | Local FS | Supabase S3 |
|---------|----------|-----------|
| Storage Location | Server disk | Cloud storage |
| Scalability | Limited | Unlimited |
| Availability | Server-dependent | Highly available |
| Backup | Manual | Built-in |
| Access | Local paths | Public URLs |
| Cost | Infrastructure | Pay-as-you-go |
| Image Deletion | Automatic filesystem | S3 delete operation |

## Error Handling

- **Invalid file extension**: Throws `InvalidRequestException` ("Only JPG and PNG image files are allowed")
- **Invalid filename**: Throws `InvalidRequestException` ("Invalid file name")
- **S3 operation failure**: Throws `RuntimeException` ("Could not store/delete image file")
- **Invalid URL format**: Throws `InvalidRequestException` ("Invalid image URL format")

## Next Steps for User

1. **Create Supabase Account**: Sign up at https://supabase.com
2. **Create Project**: Set up PostgreSQL database
3. **Create Bucket**: Create "drinks" bucket and set to Public
4. **Get Credentials**: Obtain S3 access key and secret key from Supabase
5. **Set Environment Variables**: Configure in dev/prod environments
6. **Test Upload**: Use `/drink/edit` endpoint to verify image upload
7. **Verify Storage**: Check images in Supabase Storage > drinks bucket

See [SUPABASE_STORAGE_SETUP.md](SUPABASE_STORAGE_SETUP.md) for detailed setup instructions.
