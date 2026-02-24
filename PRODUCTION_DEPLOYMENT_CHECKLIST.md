# Production Deployment TODO

## 1. EmailController Actions:
### Option A: Keep it (Recommended)
- ✅ Already secured with @Profile("dev") 
- ✅ Won't load in production
- ✅ No code changes needed

### Option B: Remove it completely  
- Delete: src/main/java/group/g/graduation/backend/common/email/EmailController.java
- Risk: Lose development testing capability

### Option C: Conditional compilation
```java
// Add to EmailController.java
@ConditionalOnProperty(name = "app.email.testing.enabled", havingValue = "true", matchIfMissing = false)
```

## 2. Security Hardening:
- [ ] Remove all secrets from application.yml
- [ ] Use environment variables
- [ ] Change default admin credentials  
- [ ] Generate strong JWT secret (256+ bits)
- [ ] Enable HTTPS only
- [ ] Add rate limiting
- [ ] Configure CORS properly

## 3. Database Production Setup:
- [ ] Create production database
- [ ] Run Flyway migrations
- [ ] Setup backup strategy
- [ ] Configure connection pooling
- [ ] Set up monitoring

## 4. Application Properties:
- [ ] Create application-prod.yml
- [ ] Set spring.profiles.active=prod
- [ ] Disable SQL logging
- [ ] Configure proper logging levels
- [ ] Set up centralized logging

## 5. Build & Deployment:
- [ ] Build with production profile: `mvn clean package -Pprod`
- [ ] Create Docker image
- [ ] Setup CI/CD pipeline  
- [ ] Configure health checks
- [ ] Setup monitoring (Prometheus/Grafana)

## 6. Infrastructure:
- [ ] Setup load balancer
- [ ] Configure SSL certificates
- [ ] Setup CDN for static files
- [ ] Configure firewall rules
- [ ] Setup monitoring alerts

## 7. Testing:
- [ ] Run all tests: `mvn test`
- [ ] Integration tests
- [ ] Load testing
- [ ] Security scanning
- [ ] Performance testing

## 8. Monitoring & Observability:
- [ ] Application logs
- [ ] Database monitoring  
- [ ] API response time monitoring
- [ ] Error rate monitoring
- [ ] Alert configuration