# Contributing to Processing Server

Thanks for your interest in contributing!

## Reporting Issues

- Check existing issues first
- Include steps to reproduce
- Mention your OS, Java version, and browser

## Submitting Changes

1. Fork the repository
2. Create a branch: `git checkout -b my-feature`
3. Make changes and test: `mvn compile test`
4. Commit with clear message
5. Push and open a Pull Request

## Development Setup

```bash
# PowerShell
git clone https://github.com/YOUR_USERNAME/processing-server.git
cd processing-server
mvn compile

# Generate keystore for HTTPS
./create-keystore.ps1

# Run
./run.ps1
```

## Code Style

- Follow existing patterns
- Keep methods focused and small
- No comments unless requested

## Questions?

Open an issue with the "question" label.