# Docker Setup Guide

This guide explains how to build and run the Coffee Shop API using Docker.

## Prerequisites

- Docker (v20.10+)
- Docker Compose (v2.0+)
- Git

## Files

- **Dockerfile**: Multi-stage build for the Spring Boot application
- **docker-compose.yml**: Orchestrates PostgreSQL and API services
- **.dockerignore**: Excludes unnecessary files from Docker context

## Quick Start

### Option 1: Using Docker Compose (Recommended)

Start both PostgreSQL and the API with a single command:

```bash
docker-compose up --build
```

This will:
1. Build the API image from the Dockerfile
2. Start PostgreSQL container
3. Start API container
4. Wait for PostgreSQL to be healthy before starting API

Access the application at: `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui.html`

### Option 2: Building and Running Separately

**Build the Docker image:**
```bash
docker build -t coffee-shop-api:latest .
```

**Run with Docker Compose (database only):**
```bash
docker-compose up postgres-db
```

**Run the API container:**
```bash
docker run -d \
  --name coffee-shop-api \
  --network coffee-network \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DB_HOST=postgres-db \
  -e DB_PORT=5432 \
  -e DB_NAME=coffee-shop-db \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres@1234 \
  coffee-shop-api:latest
```

## Configuration

### Environment Variables

The docker-compose.yml uses these environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Spring profile (dev/prod) |
| `DB_HOST` | `postgres-db` | Database hostname |
| `DB_PORT` | `5432` | Database port |
| `DB_NAME` | `coffee-shop-db` | Database name |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres@1234` | Database password |
| `SUPABASE_ENDPOINT` | (optional) | Supabase endpoint for prod |
| `SUPABASE_ACCESS_KEY` | (optional) | Supabase access key |
| `SUPABASE_SECRET_KEY` | (optional) | Supabase secret key |

### Development Setup

For development (local storage):
```bash
docker-compose up
```

### Production Setup

For production (Supabase storage), update docker-compose.yml or pass environment variables:

```bash
SPRING_PROFILES_ACTIVE=prod \
SUPABASE_ENDPOINT=https://your-supabase.com \
SUPABASE_ACCESS_KEY=your-key \
SUPABASE_SECRET_KEY=your-secret \
docker-compose up
```

Or create a `.env` file:
```env
SPRING_PROFILES_ACTIVE=prod
SUPABASE_ENDPOINT=https://your-supabase.com
SUPABASE_ACCESS_KEY=your-key
SUPABASE_SECRET_KEY=your-secret
DB_PASSWORD=your-password
```

Then run:
```bash
docker-compose up
```

## Common Commands

**Start services:**
```bash
docker-compose up
```

**Start in background:**
```bash
docker-compose up -d
```

**Stop services:**
```bash
docker-compose down
```

**Stop and remove volumes (clean database):**
```bash
docker-compose down -v
```

**View logs:**
```bash
docker-compose logs -f api
docker-compose logs -f postgres-db
```

**Rebuild image:**
```bash
docker-compose up --build
```

**Remove unused images:**
```bash
docker image prune
```

## Docker Architecture

### Multi-Stage Build

The Dockerfile uses a multi-stage build process:

1. **Builder Stage (maven:3.9-eclipse-temurin-21-alpine)**
   - Downloads Maven dependencies
   - Compiles source code
   - Creates JAR file

2. **Runtime Stage (eclipse-temurin:21-jre-alpine)**
   - Minimal JRE image
   - Copies JAR from builder
   - Runs application as non-root user

This approach reduces the final image size by ~60% compared to including Maven in the runtime image.

### Network

Both services connect to a `coffee-network` bridge network, allowing container-to-container communication by hostname.

### Health Checks

- **PostgreSQL**: Checks if database is ready using `pg_isready`
- **API**: Checks Spring Boot actuator health endpoint

The API waits for PostgreSQL to be healthy before starting.

## Troubleshooting

### Port Already in Use

If port 8080 or 5432 is already in use:

```bash
# Change ports in docker-compose.yml
# Or kill the process:
lsof -i :8080
kill -9 <PID>
```

### Container Won't Start

Check logs:
```bash
docker-compose logs api
docker-compose logs postgres-db
```

### Database Connection Issues

Verify PostgreSQL is healthy:
```bash
docker-compose exec postgres-db pg_isready -U postgres -d coffee-shop-db
```

### Build Failures

Clear Docker cache and rebuild:
```bash
docker-compose down
docker system prune
docker-compose up --build
```

## Performance Tips

1. **Use BuildKit** for faster builds:
   ```bash
   DOCKER_BUILDKIT=1 docker build -t coffee-shop-api:latest .
   ```

2. **Use .dockerignore** to reduce context size (already configured)

3. **Layer caching**: Dockerfile is optimized to cache Maven dependencies

4. **Production**: Consider using `eclipse-temurin:21-jre-alpine` for minimal runtime size

## Security Considerations

### Development
- Current setup uses hardcoded credentials for development
- Passwords are visible in logs/compose files
- Not suitable for production

### Production
- Use environment variables from secrets management
- Change default database password
- Use encrypted storage for sensitive data
- Run containers as non-root user (already implemented)
- Use health checks for monitoring

Example for production:
```bash
docker run -d \
  --name coffee-shop-api \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_PASSWORD=$(aws secretsmanager get-secret-value --secret-id db-password --query SecretString --output text) \
  coffee-shop-api:latest
```

## Pushing to Registry

### Docker Hub

```bash
# Login
docker login

# Tag image
docker tag coffee-shop-api:latest your-username/coffee-shop-api:latest

# Push
docker push your-username/coffee-shop-api:latest
```

### Private Registry

```bash
docker tag coffee-shop-api:latest your-registry.com/coffee-shop-api:latest
docker push your-registry.com/coffee-shop-api:latest
```

## Further Reading

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [Best Practices for Spring Boot Docker Images](https://spring.io/blog/2020/01/27/creating-docker-images-with-spring-boot-2-3-0-m1)

## Production Deployment with Supabase

The project includes a dedicated production configuration for deploying with Supabase storage.

### Files

- **Dockerfile**: Uses `SPRING_PROFILES_ACTIVE=prod` by default
- **docker-compose.prod.yml**: Production stack configured for Supabase
- **.env.prod.example**: Production environment template

### Setup Production

1. **Copy production environment file:**
   ```bash
   cp .env.prod.example .env.prod
   ```

2. **Update .env.prod with Supabase credentials:**
   ```env
   DB_HOST=aws-0-ap-south-1.pooler.supabase.com
   DB_PORT=5432
   DB_NAME=postgres
   DB_USERNAME=postgres.xxxxx
   DB_PASSWORD=your-password
   
   SUPABASE_BUCKET_NAME=drinks
   SUPABASE_REGION=ap-south-1
   SUPABASE_ENDPOINT=https://your-supabase-url.com
   SUPABASE_ACCESS_KEY=your-access-key
   SUPABASE_SECRET_KEY=your-secret-key
   ```

3. **Build and start production container:**
   ```bash
   # Build image
   docker build -t coffee-api:v1.0.0 .
   
   # Start with production config
   docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d
   ```

### Verify Production Setup

```bash
# Check container status
docker ps -a

# View logs
docker logs -f coffee-shop-api-prod

# Test API health
curl http://localhost:8080/actuator/health

# Test image upload
curl -X PUT http://localhost:8080/drink/edit \
  -H "Authorization: Bearer <your-token>" \
  -F "data={...}" \
  -F "image=@/path/to/image.jpg"
```

### Key Differences: Dev vs Prod

| Aspect | Dev | Prod |
|--------|-----|------|
| **Profile** | `dev` | `prod` |
| **Database** | Local PostgreSQL | Supabase PostgreSQL |
| **Image Storage** | Local filesystem (`/uploads/`) | Supabase S3 |
| **API Docs** | Enabled (Swagger UI) | Disabled |
| **Database Logs** | Debug level | Info level |
| **Container** | With PostgreSQL service | No database container |

### Production Deployment Checklist

- [ ] Supabase account and project created
- [ ] S3 bucket "drinks" created and set to public
- [ ] Database credentials obtained
- [ ] S3 access keys obtained
- [ ] `.env.prod` file created with all credentials
- [ ] Docker image built: `docker build -t coffee-api:v1.0.0 .`
- [ ] Local test passed: `docker-compose up -d`
- [ ] Supabase connectivity verified
- [ ] Image upload tested to verify storage works
- [ ] Production deployment: `docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d`
- [ ] Health check confirms: `curl http://localhost:8080/actuator/health`
- [ ] Logs monitored for errors

### Scale Production

For Kubernetes deployment, convert docker-compose to Kustomize/Helm:

```bash
# Install Kompose tool
kompose convert -f docker-compose.prod.yml -o k8s/

# Or manually create Kubernetes manifests
kubectl apply -f k8s/coffee-api-deployment.yaml
```

### Monitoring Production

Set up log aggregation and monitoring:

```bash
# View live logs
docker logs -f coffee-shop-api-prod

# Export logs
docker logs coffee-shop-api-prod > api.log

# Monitor container stats
docker stats coffee-shop-api-prod

# Set up centralized logging (example with ELK stack)
# docker-compose -f docker-compose.prod.yml -f docker-compose.logging.yml up -d
```
