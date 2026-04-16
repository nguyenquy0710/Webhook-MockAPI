# CI/CD Pipeline - WebHookMock

## 📋 Mục lục / Table of Contents
- [Overview](#overview)
- [GitHub Actions Workflows](#github-actions-workflows)
- [Build Pipeline](#build-pipeline)
- [Test Pipeline](#test-pipeline)
- [Docker Pipeline](#docker-pipeline)
- [Release Pipeline](#release-pipeline)
- [Versioning Strategy](#versioning-strategy)
- [Changelog Generation](#changelog-generation)

---

## Overview

### CI/CD Tools
- **CI/CD Platform**: GitHub Actions
- **Build Tool**: Maven
- **Container Registry**: GitHub Container Registry (GHCR)
- **Versioning**: Semantic Versioning
- **Changelog**: auto-changelog

### Workflow Triggers
- **Push to main/develop**: Build and test
- **Pull requests**: Build and test
- **Release creation**: Build, test, and deploy

---

## GitHub Actions Workflows

### Workflows Location
```
.github/workflows/
├── ci-cd.yml          # Main CI/CD pipeline
├── maven-test.yml     # Maven test workflow
└── changelog.yml      # Changelog generation
```

### ci-cd.yml
Main CI/CD workflow with build, test, and Docker deployment.

**Triggers**:
- `workflow_dispatch`: Manual trigger
- `release`: On release creation

**Jobs**:
1. `build-test`: Build and run tests
2. `release`: Upload JAR to GitHub Release
3. `docker-deploy`: Build and push Docker image

---

## Build Pipeline

### Build Steps
```yaml
jobs:
  build-test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Build with Maven
        run: ./mvnw clean package -B

      - name: Run tests
        run: ./mvnw test

      - name: Upload JAR artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-jar
          path: staging/app.jar
```

### Build Configuration
```bash
# Build command
./mvnw clean package -B

# Skip tests (for release builds)
./mvnw clean package -DskipTests
```

---

## Test Pipeline

### Test Steps
```yaml
- name: Run tests
  run: ./mvnw test

- name: List files in target/
  run: ls -lh target

- name: View dependency tree
  run: ./mvnw dependency:tree
```

### Test Commands
```bash
# Run all tests
./mvnw test

# Run single test class
./mvnw test -Dtest=UserServiceTest

# Run single test method
./mvnw test -Dtest=UserServiceTest#testRegisterUser

# Run tests with coverage
./mvnw clean test jacoco:report
```

---

## Docker Pipeline

### Docker Build Steps
```yaml
- name: Download JAR artifact
  uses: actions/download-artifact@v4
  with:
    name: app-jar

- name: Log in to GitHub Container Registry
  uses: docker/login-action@v3
  with:
    registry: ghcr.io
    username: ${{ github.actor }}
    password: ${{ secrets.GITHUB_TOKEN }}

- name: Build Docker image
  run: |
    docker build -t ghcr.io/nguyenquy0710/mockapi:${{ env.VERSION }} \
                 -t ghcr.io/nguyenquy0710/mockapi:latest .

- name: Push Docker image
  run: |
    docker push ghcr.io/nguyenquy0710/mockapi:${{ env.VERSION }}
    if [ "${{ github.event_name }}" = "release" ]; then
      docker push ghcr.io/nguyenquy0710/mockapi:latest
    fi
```

### Docker Build Configuration
```bash
# Build Docker image
docker build -t webhook-mock:latest .

# Tag for registry
docker tag webhook-mock:latest ghcr.io/nguyenquy0710/mockapi:latest

# Push to registry
docker push ghcr.io/nguyenquy0710/mockapi:latest
```

---

## Release Pipeline

### Release Steps
```yaml
release:
  needs: build-test
  runs-on: ubuntu-latest
  if: github.event_name == 'release'

  steps:
    - name: Download JAR
      uses: actions/download-artifact@v4
      with:
        name: app-jar

    - name: Upload to GitHub Release
      uses: softprops/action-gh-release@v2
      with:
        files: app.jar
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### Release Process
1. Create Git tag: `git tag v1.0.0`
2. Push tag: `git push origin v1.0.0`
3. GitHub Actions builds and deploys automatically
4. JAR uploaded to GitHub Release
5. Docker image pushed to GHCR

---

## Versioning Strategy

### Version Format
- **Format**: `1.0.{run_number}-{branch}`
- **Main branch**: `1.0.{run_number}-main`
- **Tag**: `1.0.{run_number}`
- **Dev branch**: `1.0.{run_number}-dev`

### Version Environment Variable
```yaml
env:
  VERSION: >-
    ${{ 
      github.ref_type == 'branch' && github.ref_name == 'main' 
        && format('1.0.{0}-main', github.run_number) 
        || github.ref_type == 'tag' 
        && format('1.0.{0}', github.run_number) 
        || format('1.0.{0}-dev', github.run_number) 
    }}
```

### Version File
```bash
# Create version file
echo "1.0.0" > .version.txt

# Read version
cat .version.txt
```

---

## Changelog Generation

### Changelog Workflow
```yaml
name: Update Changelog on Release

on:
  workflow_dispatch:
  release:
    types: [published]

jobs:
  update-changelog:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Set up Node.js
        uses: actions/setup-node@v3
        with:
          node-version: "18"

      - name: Install auto-changelog
        run: npm install -g auto-changelog

      - name: Generate changelog
        run: |
          auto-changelog --unreleased --latest-version --output CHANGELOG.md --commit-limit false --template keepachangelog

      - name: Commit updated changelog
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git add CHANGELOG.md
          git commit -m "chore: update changelog for release ${{ github.event.release.tag_name }}"
```

### Changelog Format
- **Format**: Keep a Changelog
- **Template**: keepachangelog
- **Location**: `CHANGELOG.md`

### Changelog Commands
```bash
# Install auto-changelog
npm install -g auto-changelog

# Generate changelog
auto-changelog --unreleased --latest-version --output CHANGELOG.md --commit-limit false --template keepachangelog
```

---

## Running Workflows Locally

### Using act
```bash
# Install act
brew install act

# Run workflow locally
act -n  # dry run
act      # execute
```

### Manual Testing
```bash
# Test build locally
./mvnw clean package

# Test Docker build locally
docker build -t webhook-mock:latest .
```

---

## Troubleshooting

### Build Fails
```bash
# Check Java version
java -version

# Clean and rebuild
./mvnw clean package

# Check for dependency issues
./mvnw dependency:tree
```

### Test Fails
```bash
# Run tests with verbose output
./mvnw test -X

# Run single test for debugging
./mvnw test -Dtest=ClassName#methodName
```

### Docker Build Fails
```bash
# Check Docker daemon
docker info

# Build with more details
docker build -t webhook-mock:latest --progress=plain .

# Check Dockerfile syntax
docker build -f Dockerfile .
```

### Release Fails
```bash
# Check GitHub token permissions
# Ensure GITHUB_TOKEN has write permissions

# Verify tag exists
git tag -l

# Push tag if missing
git push origin v1.0.0
```

---

## Best Practices

### CI/CD Best Practices
- Run tests on every build
- Build Docker images on release only
- Use semantic versioning
- Generate changelog on release
- Upload artifacts for release
- Use caching for dependencies

### Security Best Practices
- Never commit secrets
- Use GitHub Secrets for sensitive data
- Scan Docker images for vulnerabilities
- Use minimal Docker base images
- Run containers as non-root user

### Performance Best Practices
- Cache Maven dependencies
- Use matrix builds for parallel testing
- Optimize Docker layer caching
- Use multi-stage Docker builds

---

## Monitoring

### Workflow Monitoring
- Check GitHub Actions tab for workflow status
- View workflow logs for errors
- Monitor build times
- Track test coverage

### Alerting
- Configure workflow failure notifications
- Monitor build success rate
- Track Docker build times
- Monitor test pass rate

---

## Next Steps

- [ ] [Deployment Guide](./DEPLOYMENT.md) - Deploy to production
- [ ] [Architecture](./ARCHITECTURE.md) - Understand system design
- [ ] [User Guide](./USER-GUIDE.md) - Using WebHookMock features
