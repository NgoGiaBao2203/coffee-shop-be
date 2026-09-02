---
description: "Domain-driven Coffee Shop API development with automatic ERD loading and folder scaffolding. Use `--erd` to auto-load database schema, `--domain [name]` to create/reuse domain folders."
tools: [read, edit, search, execute]
user-invocable: true
hooks:
  UserPromptSubmit:
    - type: command
      command: |
        #!/bin/bash
        # Handle special flags: --erd and --domain [name]
      
        # Get the input from stdin (Copilot will pipe the prompt here)
        input="$1"
        erd_file="/home/chrisnguyen/coffee-shop/coffee-shop-be/api/.github/agents/ERD.md"
        api_root="/home/chrisnguyen/coffee-shop/coffee-shop-be/api/src/main/java/coffee/api"
      
        # Check for --erd flag
        if [[ "$input" == *"--erd"* ]]; then
          if [[ -f "$erd_file" ]]; then
            echo "📋 Loading ERD context from .github/agents/ERD.md"
            echo ""
          fi
        fi
      
        # Check for --domain [name] flag
        if [[ "$input" =~ --domain[[:space:]]+([a-zA-Z0-9_-]+) ]]; then
          domain_name="${BASH_REMATCH[1]}"
          domain_path="$api_root/$domain_name"
        
          # Create domain folder if it doesn't exist
          if [[ ! -d "$domain_path" ]]; then
            mkdir -p "$domain_path"
            echo "✓ Created domain folder: $domain_name"
          else
            echo "✓ Reusing existing domain folder: $domain_name"
          fi
        
          # Create subdirectories if they don't exist
          for subdir in controllers services_interface services_implement; do
            mkdir -p "$domain_path/$subdir"
          done
        
          echo "✓ Domain structure ready at: $domain_path"
          echo ""
        fi
      
        exit 0
      timeout: 5
      cwd: "/home/chrisnguyen/coffee-shop/coffee-shop-be/api"
---
# Domain-Driven Coffee Shop API Development

You are a specialist at developing features for the Coffee Shop API using domain-driven patterns.

## Special Flags

### `--erd`

Automatically loads the Entity Relationship Diagram from `.github/agents/ERD.md` to provide context about the database schema, relationships, and constraints.

**Usage:**

```
@coffee-api-domain-dev --erd
What tables should I query for getting all active categories?
```

### `--domain [name]`

Creates or reuses a domain folder structure for a new feature domain (category, drink, staff, invoice, etc.).

**Usage:**

```
@coffee-api-domain-dev --domain order
Add GET /order/search endpoint with pagination
```

This will:

- Create `/src/main/java/coffee/api/order/` (if not exists)
- Create subdirectories: `controllers/`, `services_interface/`, `services_implement/`
- Ready for adding OrderController, OrderService, etc.

## How It Works

1. **Flag Detection**: The agent detects `--erd` or `--domain [name]` in your prompt
2. **Automatic Setup**:
   - `--erd` loads schema context from ERD.md
   - `--domain [name]` scaffolds the folder structure
3. **Development**: Proceed with your feature request; the context is ready

## Best Practices

- Use `--erd` when you need to understand data relationships before writing queries
- Use `--domain [name]` at the start of developing a new feature area
- Combine both: `--erd --domain order Add new order endpoints`

## Integration with Other Tools

- Use with `/add-api-endpoint` skill for complete endpoint workflow
- Use `@mybatis-query-specialist` for complex queries with ERD context
- Use `/code-review-conventions` to validate your domain code

---

## Example Workflows

### Create a new domain with context

```
@coffee-api-domain-dev --erd --domain promotion
I need to create GET /promotion/list and POST /promotion/create endpoints.
```

### Query existing data

```
@coffee-api-domain-dev --erd
Write a MyBatis query that joins invoices with their details and shop names.
```

### Start implementing in existing domain

```
@coffee-api-domain-dev --domain staff
Add PUT /staff/update endpoint that only OWNER can use.
```
