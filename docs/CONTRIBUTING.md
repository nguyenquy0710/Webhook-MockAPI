# Contributing - WebHookMock

## 📋 Mục lục / Table of Contents
- [Code of Conduct](#code-of-conduct)
- [How to Contribute](#how-to-contribute)
- [Development Setup](#development-setup)
- [Coding Standards](#coding-standards)
- [Commit Messages](#commit-messages)
- [Pull Requests](#pull-requests)
- [Testing](#testing)
- [Documentation](#documentation)

---

## Code of Conduct

### Our Pledge
We as members, contributors, and leaders pledge to make participation in our community a harassment-free experience for everyone.

### Our Standards
- Be respectful and inclusive
- Accept constructive criticism
- Focus on what is best for the community
- Show empathy towards other community members

### Enforcement
Violations of the code of conduct may be reported by contacting the project team.

---

## How to Contribute

### Report Bugs
1. Check if the bug has already been reported
2. Create a new issue with:
   - Clear title
   - Description of the problem
   - Steps to reproduce
   - Expected behavior
   - Environment details

### Suggest Features
1. Check if the feature has already been suggested
2. Create a new issue with:
   - Clear title
   - Description of the feature
   - Use cases
   - Benefits

### Fix Bugs
1. Find an open bug issue
2. Comment to express interest
3. Create a pull request with the fix

### Implement Features
1. Find an open feature issue
2. Comment to express interest
3. Create a pull request with the implementation

---

## Development Setup

### Prerequisites
- Java 17 or higher
- Maven 3.8.8+
- Git

### Clone Repository
```bash
git clone https://github.com/nguyenquy0710/Webhook-MockAPI.git
cd Webhook-MockAPI
```

### Build Project
```bash
./mvnw clean package
```

### Run Application
```bash
./mvnw spring-boot:run
```

### Access Application
- **URL**: http://localhost:8081
- **H2 Console**: http://localhost:8081/h2-console

---

## Coding Standards

### Java Code Style
- Follow **Google Java Style Guide**
- Use **4 spaces** for indentation
- Use **camelCase** for methods and variables
- Use **PascalCase** for classes
- Use **UPPER_SNAKE_CASE** for constants

### Import Order
1. `java.*`
2. `javax.*`
3. `org.*`
4. `com.*`
5. `vn.autobot.webhook.*`

### File Organization
- One class per file
- File name matches class name
- Package declaration at top
- Imports after package declaration

### Code Organization
```java
// 1. Package declaration
package vn.autobot.webhook;

// 2. Imports
import ...

// 3. Class declaration
public class ClassName {
    // 4. Constants
    private static final String CONSTANT = "value";
    
    // 5. Fields
    private Field field;
    
    // 6. Constructor
    public ClassName() {}
    
    // 7. Methods
    public void method() {}
}
```

---

## Commit Messages

### Format
```
<type>: <subject>

<body>

<footer>
```

### Types
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

### Examples
```
feat: add user registration endpoint
fix: resolve NPE in request logging
docs: update README with installation guide
refactor: extract service layer logic
test: add unit tests for user service
```

### Best Practices
- Use imperative mood ("Add" not "Added")
- Limit first line to 72 characters
- Use body for detailed explanation
- Reference issue numbers in footer

---

## Pull Requests

### PR Template
```markdown
## Description
[Describe your changes]

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests passed
- [ ] Integration tests passed
- [ ] Manual testing completed

## Checklist
- [ ] Code follows style guidelines
- [ ] Comments added for complex logic
- [ ] Documentation updated
- [ ] Tests added/updated
```

### PR Process
1. Create feature branch from `develop`
2. Make changes and commit
3. Run tests: `./mvnw test`
4. Push branch to remote
5. Create pull request to `develop`
6. Address review comments
7. Merge after approval

### PR Guidelines
- Keep PRs small and focused
- One feature/fix per PR
- Include tests for new code
- Update documentation if needed
- Reference related issues

---

## Testing

### Unit Tests
```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=UserServiceTest

# Run specific test method
./mvnw test -Dtest=UserServiceTest#testRegisterUser
```

### Test Coverage
- Aim for > 80% code coverage
- Test all public methods
- Test edge cases and error conditions
- Use meaningful test names

### Test Structure
```java
@Test
void testMethodName_State_ExpectedBehavior() {
    // Given - Setup
    // When - Execute
    // Then - Verify
}
```

---

## Documentation

### Code Comments
- Add comments for complex logic
- Explain "why" not "what"
- Update comments when code changes
- Use clear and concise language

### API Documentation
- Update Swagger annotations
- Add example requests/responses
- Document parameters and responses
- Include error responses

### User Documentation
- Update README.md if needed
- Update user guides
- Add examples and tutorials
- Document new features

---

## Questions?

### Getting Help
- Check existing documentation
- Search open issues
- Ask in GitHub Discussions
- Contact maintainers

### Becoming a Maintainer
- Contribute significantly
- Help review PRs
- Help answer questions
- Demonstrate commitment

---

## Acknowledgments

Thank you for contributing to WebHookMock! Your efforts help make this project better for everyone.

---

*Document Version: 1.0*  
*Last Updated: 2026-04-16*
