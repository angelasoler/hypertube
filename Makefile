.PHONY: help build up down restart logs clean test security-check env-setup build-services

# Colors for output
GREEN  := \033[0;32m
YELLOW := \033[0;33m
RED    := \033[0;31m
RESET  := \033[0m

# Default target
.DEFAULT_GOAL := help

##@ General

help: ## Display this help message
	@echo "$(GREEN)HyperTube - Makefile Commands$(RESET)"
	@echo ""
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make $(YELLOW)<target>$(RESET)\n"} /^[a-zA-Z_-]+:.*?##/ { printf "  $(YELLOW)%-20s$(RESET) %s\n", $$1, $$2 } /^##@/ { printf "\n$(GREEN)%s$(RESET)\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

##@ Environment Setup

env-setup: ## Setup environment file from template
	@if [ ! -f .env ]; then \
		echo "$(YELLOW)Creating .env from .env.example...$(RESET)"; \
		cp .env.example .env; \
		echo "$(RED)WARNING: Please configure secure passwords in .env before starting!$(RESET)"; \
		echo "$(YELLOW)Run 'make generate-passwords' to generate secure passwords$(RESET)"; \
	else \
		echo "$(GREEN).env file already exists$(RESET)"; \
	fi

generate-passwords: ## Generate secure passwords for .env file
	@echo "$(GREEN)Generating secure passwords...$(RESET)"
	@echo ""
	@echo "$(YELLOW)Add these to your .env file:$(RESET)"
	@echo "DB_PASSWORD=$$(openssl rand -base64 32)"
	@echo "REDIS_PASSWORD=$$(openssl rand -base64 32)"
	@echo "RABBITMQ_PASSWORD=$$(openssl rand -base64 32)"
	@echo "JWT_SECRET=$$(openssl rand -base64 64)"
	@echo ""
	@echo "$(YELLOW)Or run: make auto-configure-env$(RESET)"

auto-configure-env: ## Automatically configure .env with secure passwords
	@if [ ! -f .env ]; then \
		echo "$(YELLOW)Creating .env from template...$(RESET)"; \
		cp .env.example .env; \
	fi
	@echo "$(GREEN)Configuring secure passwords in .env...$(RESET)"
	@sed -i "s/DB_PASSWORD=.*/DB_PASSWORD=$$(openssl rand -base64 32)/" .env
	@sed -i "s/REDIS_PASSWORD=.*/REDIS_PASSWORD=$$(openssl rand -base64 32)/" .env
	@sed -i "s/RABBITMQ_PASSWORD=.*/RABBITMQ_PASSWORD=$$(openssl rand -base64 32)/" .env
	@sed -i "s/JWT_SECRET=.*/JWT_SECRET=$$(openssl rand -base64 64)/" .env
	@echo "$(GREEN)Passwords configured successfully!$(RESET)"
	@echo "$(YELLOW)Review .env file before starting services$(RESET)"

security-check: ## Run security validation checks
	@echo "$(GREEN)Running security checks...$(RESET)"
	@chmod +x scripts/check-security.sh
	@./scripts/check-security.sh

##@ Docker - Development

build: ## Build all Docker images
	@echo "$(GREEN)Building Docker images...$(RESET)"
	docker compose build

build-no-cache: ## Build all Docker images without cache
	@echo "$(GREEN)Building Docker images (no cache)...$(RESET)"
	docker compose build --no-cache

up: ## Start all services (detached)
	@echo "$(GREEN)Starting all services...$(RESET)"
	docker compose up -d

up-dev: ## Start all services with dev profile
	@echo "$(GREEN)Starting all services (dev mode)...$(RESET)"
	SPRING_PROFILES_ACTIVE=dev docker compose up -d

up-build: ## Build and start all services
	@echo "$(GREEN)Building and starting all services...$(RESET)"
	docker compose up -d --build

down: ## Stop all services
	@echo "$(YELLOW)Stopping all services...$(RESET)"
	docker compose down

down-volumes: ## Stop all services and remove volumes (WARNING: deletes data)
	@echo "$(RED)Stopping all services and removing volumes...$(RESET)"
	docker compose down -v

restart: ## Restart all services
	@echo "$(YELLOW)Restarting all services...$(RESET)"
	docker compose restart

##@ Docker - Service Management

start-infra: ## Start only infrastructure services (postgres, redis, rabbitmq)
	@echo "$(GREEN)Starting infrastructure services...$(RESET)"
	docker compose up -d postgres redis rabbitmq

start-eureka: ## Start Eureka service discovery
	@echo "$(GREEN)Starting Eureka server...$(RESET)"
	docker compose up -d eureka-server

start-gateway: ## Start API Gateway
	@echo "$(GREEN)Starting API Gateway...$(RESET)"
	docker compose up -d api-gateway

start-users: ## Start User Management service
	@echo "$(GREEN)Starting User Management service...$(RESET)"
	docker compose up -d user-management

start-streaming: ## Start Video Streaming service
	@echo "$(GREEN)Starting Video Streaming service...$(RESET)"
	docker compose up -d video-streaming

start-search: ## Start Search/Library service
	@echo "$(GREEN)Starting Search Library service...$(RESET)"
	docker compose up -d search-library

restart-gateway: ## Restart API Gateway
	@echo "$(YELLOW)Restarting API Gateway...$(RESET)"
	docker compose restart api-gateway

restart-eureka: ## Restart Eureka server
	@echo "$(YELLOW)Restarting Eureka server...$(RESET)"
	docker compose restart eureka-server

##@ Logs & Monitoring

logs: ## Show logs for all services
	docker compose logs -f

logs-gateway: ## Show API Gateway logs
	docker compose logs -f api-gateway

logs-eureka: ## Show Eureka server logs
	docker compose logs -f eureka-server

logs-users: ## Show User Management logs
	docker compose logs -f user-management

logs-streaming: ## Show Video Streaming logs
	docker compose logs -f video-streaming

logs-search: ## Show Search Library logs
	docker compose logs -f search-library

logs-postgres: ## Show PostgreSQL logs
	docker compose logs -f postgres

logs-redis: ## Show Redis logs
	docker compose logs -f redis

logs-rabbitmq: ## Show RabbitMQ logs
	docker compose logs -f rabbitmq

ps: ## Show status of all services
	@docker compose ps

stats: ## Show resource usage statistics
	@docker stats --no-stream

##@ Database

db-shell: ## Open PostgreSQL shell
	@docker compose exec postgres psql -U $${DB_USER:-hypertube_user} -d $${DB_NAME:-hypertube}

db-init: ## Initialize database schemas
	@echo "$(GREEN)Initializing database...$(RESET)"
	@docker compose exec postgres psql -U $${DB_USER:-hypertube_user} -d $${DB_NAME:-hypertube} -f /docker-entrypoint-initdb.d/init-db.sql

db-backup: ## Backup database to ./backups/
	@mkdir -p backups
	@echo "$(GREEN)Backing up database...$(RESET)"
	@docker compose exec -T postgres pg_dump -U $${DB_USER:-hypertube_user} $${DB_NAME:-hypertube} > backups/hypertube_$$(date +%Y%m%d_%H%M%S).sql
	@echo "$(GREEN)Backup completed$(RESET)"

db-restore: ## Restore database from latest backup
	@echo "$(YELLOW)Restoring from latest backup...$(RESET)"
	@docker compose exec -T postgres psql -U $${DB_USER:-hypertube_user} -d $${DB_NAME:-hypertube} < $$(ls -t backups/*.sql | head -1)
	@echo "$(GREEN)Restore completed$(RESET)"

##@ Cache & Queue

redis-cli: ## Open Redis CLI
	@docker compose exec redis redis-cli -a $${REDIS_PASSWORD}

redis-flush: ## Flush all Redis cache (WARNING: clears all cache)
	@echo "$(RED)Flushing all Redis cache...$(RESET)"
	@docker compose exec redis redis-cli -a $${REDIS_PASSWORD} FLUSHALL

rabbitmq-ui: ## Open RabbitMQ Management UI
	@echo "$(GREEN)RabbitMQ UI: http://localhost:15672$(RESET)"
	@echo "Username: $${RABBITMQ_USER:-hypertube}"
	@xdg-open http://localhost:15672 2>/dev/null || open http://localhost:15672 2>/dev/null || echo "Visit http://localhost:15672"

##@ Build & Development

build-services: ## Build all Spring Boot services (Maven)
	@echo "$(GREEN)Building all services...$(RESET)"
	@for service in services/*/; do \
		if [ -f "$$service/pom.xml" ]; then \
			echo "$(YELLOW)Building $$(basename $$service)...$(RESET)"; \
			cd $$service && mvn clean package -DskipTests && cd ../..; \
		fi \
	done
	@echo "$(GREEN)All services built successfully$(RESET)"

build-gateway: ## Build API Gateway service
	@echo "$(GREEN)Building API Gateway...$(RESET)"
	@cd services/api-gateway && mvn clean package -DskipTests

build-eureka: ## Build Eureka server
	@echo "$(GREEN)Building Eureka server...$(RESET)"
	@cd services/eureka-server && mvn clean package -DskipTests

build-users: ## Build User Management service
	@echo "$(GREEN)Building User Management service...$(RESET)"
	@cd services/user-management && mvn clean package -DskipTests

test-all: ## Run all tests
	@echo "$(GREEN)Running all tests...$(RESET)"
	@for service in services/*/; do \
		if [ -f "$$service/pom.xml" ]; then \
			echo "$(YELLOW)Testing $$(basename $$service)...$(RESET)"; \
			cd $$service && mvn test && cd ../..; \
		fi \
	done

test-gateway: ## Run API Gateway tests
	@cd services/api-gateway && mvn test

test-eureka: ## Run Eureka server tests
	@cd services/eureka-server && mvn test

test-users: ## Run User Management tests
	@cd services/user-management && mvn test

test-infrastructure: ## Run infrastructure tests
	@echo "$(GREEN)Running infrastructure tests...$(RESET)"
	@chmod +x scripts/test-infrastructure.sh
	@./scripts/test-infrastructure.sh

##@ Cleanup

clean: ## Clean build artifacts and Docker resources
	@echo "$(YELLOW)Cleaning build artifacts...$(RESET)"
	@find services -type d -name target -exec rm -rf {} + 2>/dev/null || true
	@echo "$(GREEN)Clean completed$(RESET)"

clean-docker: ## Remove all Docker images and containers
	@echo "$(RED)Removing Docker containers and images...$(RESET)"
	docker compose down --rmi all
	@echo "$(GREEN)Docker cleanup completed$(RESET)"

clean-all: ## Clean everything (artifacts, Docker, volumes)
	@echo "$(RED)Cleaning everything...$(RESET)"
	@$(MAKE) down-volumes
	@$(MAKE) clean
	@$(MAKE) clean-docker
	@echo "$(GREEN)Full cleanup completed$(RESET)"

prune: ## Prune unused Docker resources
	@echo "$(YELLOW)Pruning unused Docker resources...$(RESET)"
	docker system prune -f
	@echo "$(GREEN)Prune completed$(RESET)"

##@ Service URLs

urls: ## Display all service URLs
	@echo "$(GREEN)Service URLs:$(RESET)"
	@echo "  Eureka Dashboard:     http://localhost:8761"
	@echo "  API Gateway:          http://localhost:8080"
	@echo "  RabbitMQ Management:  http://localhost:15672"
	@echo "  PostgreSQL:           localhost:5432"
	@echo "  Redis:                localhost:6379"
	@echo ""
	@echo "$(YELLOW)Service Endpoints:$(RESET)"
	@echo "  User Management:      http://localhost:8080/api/v1/users"
	@echo "  Search Library:       http://localhost:8080/api/v1/search"
	@echo "  Video Streaming:      http://localhost:8080/api/v1/streaming"

health: ## Check health of all services
	@echo "$(GREEN)Checking service health...$(RESET)"
	@echo ""
	@echo "$(YELLOW)Eureka:$(RESET)"
	@curl -s http://localhost:8761/actuator/health | grep -q UP && echo "  $(GREEN)✓ UP$(RESET)" || echo "  $(RED)✗ DOWN$(RESET)"
	@echo "$(YELLOW)API Gateway:$(RESET)"
	@curl -s http://localhost:8080/actuator/health | grep -q UP && echo "  $(GREEN)✓ UP$(RESET)" || echo "  $(RED)✗ DOWN$(RESET)"
	@echo "$(YELLOW)PostgreSQL:$(RESET)"
	@docker compose exec -T postgres pg_isready -U $${DB_USER:-hypertube_user} > /dev/null 2>&1 && echo "  $(GREEN)✓ UP$(RESET)" || echo "  $(RED)✗ DOWN$(RESET)"
	@echo "$(YELLOW)Redis:$(RESET)"
	@docker compose exec -T redis redis-cli -a $${REDIS_PASSWORD} ping > /dev/null 2>&1 && echo "  $(GREEN)✓ UP$(RESET)" || echo "  $(RED)✗ DOWN$(RESET)"
	@echo "$(YELLOW)RabbitMQ:$(RESET)"
	@docker compose exec -T rabbitmq rabbitmq-diagnostics -q ping > /dev/null 2>&1 && echo "  $(GREEN)✓ UP$(RESET)" || echo "  $(RED)✗ DOWN$(RESET)"

##@ Quick Start

quickstart: env-setup auto-configure-env security-check up ## Complete quickstart: setup env, configure passwords, and start services
	@echo ""
	@echo "$(GREEN)======================================$(RESET)"
	@echo "$(GREEN)  HyperTube Started Successfully!$(RESET)"
	@echo "$(GREEN)======================================$(RESET)"
	@echo ""
	@$(MAKE) urls
	@echo ""
	@echo "$(YELLOW)Run 'make logs' to view logs$(RESET)"
	@echo "$(YELLOW)Run 'make health' to check service health$(RESET)"

dev: ## Start development environment
	@echo "$(GREEN)Starting development environment...$(RESET)"
	@$(MAKE) env-setup
	@$(MAKE) up-dev
	@$(MAKE) logs
