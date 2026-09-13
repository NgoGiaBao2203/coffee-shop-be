# Supabase Storage Setup Guide

## Overview
The application now uses Supabase Storage (S3-compatible) for storing drink images instead of local file system storage.

## Prerequisites
1. Supabase account (https://supabase.com)
2. A Supabase project created with PostgreSQL database

## Setup Steps

### 1. Create a Storage Bucket in Supabase
- Log in to [Supabase Dashboard](https://supabase.com/dashboard)
- Select your project
- Go to **Storage** > **Buckets**
- Click **Create a new bucket**
- Name: `drinks`
- Access policy: **Public** (to allow serving images)
- Click **Create bucket**

### 2. Generate Access Keys
- Go to **Project Settings** > **API**
- You'll need:
  - **Project URL**: Copy this from the API section (e.g., `https://xxxx.supabase.co`)
  - **Service Role Secret**: Copy this from the API section

### 3. Get Supabase S3 Credentials
- Go to **Project Settings** > **Storage** > **S3 Credentials**
- Copy:
  - **Access Key ID**: This is your `SUPABASE_ACCESS_KEY`
  - **Secret Access Key**: This is your `SUPABASE_SECRET_KEY`

### 4. Configure Environment Variables

#### For Local Development (Dev Profile)
Add these to your environment or `.env` file (or pass as JVM arguments):
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=coffee-shop-db
export DB_USERNAME=postgres
export DB_PASSWORD=postgres@1234

export SUPABASE_BUCKET_NAME=drinks
export SUPABASE_REGION=ap-south-1
export SUPABASE_ENDPOINT=https://aws-0-ap-south-1.pooler.supabase.com
export SUPABASE_PUBLIC_URL=https://xauxrvolraonrrxavkrt.storage.supabase.co
export SUPABASE_ACCESS_KEY=<your_access_key_id>
export SUPABASE_SECRET_KEY=<your_secret_access_key>
```

#### For Production (Prod Profile)
Set environment variables in your production environment:
```bash
export DB_HOST=aws-0-ap-south-1.pooler.supabase.com
export DB_PORT=5432
export DB_NAME=postgres
export DB_USERNAME=postgres.ldamaltfehzopoqfywpa
export DB_PASSWORD=<your_database_password>

export SUPABASE_BUCKET_NAME=drinks
export SUPABASE_REGION=ap-south-1
export SUPABASE_ENDPOINT=https://aws-0-ap-south-1.pooler.supabase.com
export SUPABASE_PUBLIC_URL=https://ldamaltfehzopoqfywpa.storage.supabase.co
export SUPABASE_ACCESS_KEY=<your_access_key_id>
export SUPABASE_SECRET_KEY=<your_secret_access_key>
```

### 5. Start the Application

#### Development:
```bash
./scripts/run-spring-boot.sh
```

#### Production:
```bash
java -Dspring.profiles.active=prod -jar api-0.0.1-SNAPSHOT.jar
```

## How It Works

### Image Upload Flow
1. User uploads a drink image via `PUT /drink/edit` endpoint
2. `EditDrinksServiceImpl` validates the request and calls `IFileStorageService.storeDrinkImage()`
3. `FileStorageServiceImpl` uploads the image to Supabase Storage using S3 SDK
4. Returns the public URL: `https://{endpoint}/storage/v1/object/public/drinks/{filename}`
5. URL is stored in the database

### Image Replacement Flow
1. When editing a drink, if a new image is provided:
2. Old image URL is extracted from the database
3. Old image is deleted from Supabase Storage using `IFileStorageService.deleteDrinkImage()`
4. New image is uploaded
5. New URL is stored in the database

### Image Not Replaced
1. If no new image is provided, old image is kept as-is
2. No deletion happens

## Testing the Integration

### 1. Test Local Upload (Dev):
```bash
curl -X PUT http://localhost:8080/drink/edit \
  -H "Authorization: Bearer <your_token>" \
  -F "data={\"drinkId\": 1, ...}" \
  -F "image=@/path/to/image.jpg"
```

### 2. Verify in Supabase Dashboard:
- Go to **Storage** > **drinks** bucket
- Should see uploaded images organized by timestamp

## Troubleshooting

### "Could not store image file" Error
- Check Supabase access credentials
- Verify bucket exists and is public
- Ensure S3 credentials have proper permissions

### "Invalid image URL format" Error
- Database URL doesn't match expected format
- Check that image was stored with correct path

### Connection Timeouts
- Verify Supabase endpoint URL is correct
- Check network connectivity to Supabase

## Notes
- Images are stored in `drinks/{filename}` path in the bucket
- Old images are automatically deleted when replaced
- File extensions must be `.jpg` or `.png`
- Maximum file size depends on Supabase plan
