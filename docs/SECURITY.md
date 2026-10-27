# Security Guide - WebHookMock

## 📋 Mục lục / Table of Contents
- [Overview](#overview)
- [Authentication](#authentication)
- [Authorization](#authorization)
- [Password Security](#password-security)
- [Input Validation](#input-validation)
- [Data Protection](#data-protection)
- [API Security](#api-security)
- [Security Best Practices](#security-best-practices)
- [Security Checklist](#security-checklist)

---

## Overview

### Security Features
- **Password Hashing**: BCrypt with cost factor 12
- **Session Management**: Secure cookies with HttpOnly flag
- **CSRF Protection**: Enabled for forms, disabled for webhook APIs
- **Role-Based Access Control**: USER/ADMIN roles
- **Data Isolation**: Users can only access their own data
- **Input Validation**: Server-side validation for all inputs

---

## Authentication

### Password Hashing
- **Algorithm**: BCrypt
- **Cost Factor**: 12
- **Minimum Length**: 6 characters

### Session Management
- **Cookie Name**: JSESSIONID
- **HttpOnly**: true
- **Secure**: true (in production)
- **Timeout**: Configurable

### Login Flow
```
1. User submits login form
2. System validates credentials
3. System creates authentication session
4. System sets secure cookie
5. System redirects to dashboard
```

---

## Authorization

### Role-Based Access Control

| Role | Permissions |
|------|-------------|
| **USER** | Create/manage own webhooks, view own logs |
| **ADMIN** | All USER permissions + user management |

### Permission Checks
- **User-level**: Verify ownership before accessing resources
- **Admin-level**: Verify ADMIN role for admin endpoints
- **Data isolation**: Users cannot access other users' data

### Protected Endpoints

| Endpoint | Required Role |
|----------|---------------|
| `/admin/**` | ADMIN |
| `/api/configs/@{username}` | USER (own username) |
| `/api/logs/@{username}` | USER (own username) |

---

## Password Security

### Password Requirements
- Minimum 6 characters
- No maximum length
- No special character requirements (user-friendly)

### Password Storage
- **Hashing**: BCrypt with cost factor 12
- **Storage**: Database (never plain text)
- **Comparison**: BCrypt matcher

### Password Change
- Verify current password
- Validate new password
- Require password confirmation
- Update hash in database

---

## Input Validation

### Validation Rules

| Field | Rules |
|-------|-------|
| **Username** | 3-20 chars, alphanumeric + underscore/hyphen |
| **Email** | Valid RFC 5322 format |
| **Password** | Minimum 6 characters |
| **HTTP Method** | GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD |
| **Status Code** | 100-599 |
| **Delay** | 0-60000ms |

### SQL Injection Prevention
- **Parameterized Queries**: All database queries use parameters
- **ORM**: Hibernate for database operations
- **Input Sanitization**: Server-side validation

### XSS Prevention
- **Output Encoding**: Thymeleaf auto-escaping
- **Content Security Policy**: Security headers
- **Input Validation**: Sanitize user inputs

---

## Data Protection

### Password Protection
- **Never Log**: Passwords are never logged
- **Never Store Plain Text**: Always hash with BCrypt
- **Never Send**: Passwords are never sent in responses

### Sensitive Data
- **API Keys**: Not stored in database
- **Tokens**: Not stored in database
- **Credentials**: Not stored in database

### Data Isolation
- **User-level**: Data isolated per user
- **Database**: Single database per instance
- **File System**: Separate data directories

---

## API Security

### Webhook API Security
- **CSRF Disabled**: For easier integration
- **Ownership Verification**: Verify user ownership
- **Input Validation**: Validate all inputs
- **Rate Limiting**: Planned for future

### Authentication for Webhook APIs
- **Session-based**: JSESSIONID cookie
- **CSRF Disabled**: For stateless APIs
- **CORS**: Configured for cross-origin requests

### Security Headers
- **X-Frame-Options**: SAMEORIGIN
- **X-Content-Type-Options**: nosniff
- **X-XSS-Protection**: 1; mode=block
- **Referrer-Policy**: strict-origin-when-cross-origin

---

## Security Best Practices

### Development Best Practices
- **Never Commit Secrets**: Use environment variables
- **Validate All Inputs**: Server-side validation
- **Use Parameterized Queries**: Prevent SQL injection
- **Encode Output**: Prevent XSS
- **Log Security Events**: Track security-related events

### Deployment Best Practices
- **Use HTTPS**: Enable SSL/TLS
- **Set Secure Cookies**: HttpOnly and Secure flags
- **Configure CORS**: Restrict origins
- **Enable Rate Limiting**: Prevent abuse
- **Regular Security Scans**: Scan for vulnerabilities

### Monitoring Best Practices
- **Log Security Events**: Track failed logins, unauthorized access
- **Monitor Access Patterns**: Detect anomalies
- **Alert on Suspicious Activity**: Set up alerts
- **Review Logs Regularly**: Regular security reviews

---

## Security Checklist

### Development Checklist
- [ ] All passwords hashed with BCrypt
- [ ] All inputs validated server-side
- [ ] Parameterized queries used
- [ ] Output encoding applied
- [ ] Security headers set
- [ ] No secrets in code
- [ ] No sensitive data in logs

### Deployment Checklist
- [ ] HTTPS enabled
- [ ] Secure cookies configured
- [ ] CORS configured
- [ ] Rate limiting enabled
- [ ] Security headers set
- [ ] Database password strong
- [ ] H2 console disabled in production

### Monitoring Checklist
- [ ] Security events logged
- [ ] Failed logins tracked
- [ ] Unauthorized access tracked
- [ ] Alerts configured
- [ ] Logs reviewed regularly

---

## Security Incident Response

### Breach Response
1. **Isolate**: Isolate affected systems
2. **Change**: Change all credentials
3. **Patch**: Patch vulnerabilities
4. **Notify**: Notify affected users
5. **Review**: Post-incident review

### Lost Password
1. **Verify**: Verify user identity
2. **Reset**: Reset password
3. **Notify**: Notify user
4. **Log**: Log the incident

### Suspicious Activity
1. **Investigate**: Investigate the activity
2. **Block**: Block suspicious IPs if needed
3. **Review**: Review access logs
4. **Report**: Report to security team

---

## Security Resources

### Documentation
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [BCrypt Documentation](https://en.wikipedia.org/wiki/Bcrypt)

### Tools
- [OWASP ZAP](https://owasp.org/www-project-zap/)
- [Nessus](https://www.tenable.com/products/nessus)
- [Burp Suite](https://portswigger.net/burp)

---

## Next Steps

- [ ] [Deployment Guide](./DEPLOYMENT.md) - Deploy securely
- [ ] [Architecture](./ARCHITECTURE.md) - Understand security design
- [ ] [User Guide](./USER-GUIDE.md) - Use securely
