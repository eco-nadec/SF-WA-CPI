# Contributing to SF-WA-CPI

Thank you for your interest in contributing to the SuccessFactors Work Assignment & Timesheet Conflict Resolution project!

## 📋 Before You Start

1. Read the [ANALYSIS_INDEX.md](ANALYSIS_INDEX.md) to understand the project structure
2. Review [IFLOW_ARCHITECTURE_DIAGRAM.md](IFLOW_ARCHITECTURE_DIAGRAM.md) to understand the flow relationships
3. Check [IFLOW_NAMING_GUIDE.md](IFLOW_NAMING_GUIDE.md) for naming conventions

## 🔧 Development Guidelines

### Creating New Integration Flows

1. **Follow the naming convention:**
   - Display Name: "WA Conflict Resolution - Your Feature"
   - Technical Name: `WA_TS_YourFeature_Action`
   - See [NAMING_CONVENTION_EXPLAINED.md](NAMING_CONVENTION_EXPLAINED.md)

2. **Use the correct prefix:**
   - `SF_` for SuccessFactors API calls
   - `WA_TS_` for Work Assignment & Timesheet business logic
   - `Test_` for testing flows

3. **Document your flow:**
   - Add description in the .iflw file
   - Update IFLOW_NAMING_GUIDE.md
   - Update IFLOW_ARCHITECTURE_DIAGRAM.md if it affects flow relationships

### Modifying Existing Flows

1. **Never modify production flows directly**
   - Create a copy with version suffix (e.g., `WA_TS_Conflict_Analyze_v2`)
   - Test thoroughly in DEV
   - Update references
   - Deploy to production
   - Remove old version after verification

2. **Update version in MANIFEST.MF**
   ```
   Bundle-Version: 7.11.2025  # Increment version
   ```

3. **Document changes:**
   - Add comments in Groovy scripts
   - Update relevant documentation
   - Include change reason in commit message

### Groovy Script Guidelines

1. **Always include header comment:**
   ```groovy
   /*
    * Flow: WA_TS_Conflict_Analyze
    * Purpose: Detects time overlaps between WA and TS
    * Author: [Your Name]
    * Date: YYYY-MM-DD
    * Version: 1.0
    */
   ```

2. **Use meaningful variable names:**
   ```groovy
   // Good
   def workAssignmentStartTime = parseTime(wa.startTime)

   // Bad
   def t1 = parseTime(wa.startTime)
   ```

3. **Add logging for debugging:**
   ```groovy
   def log = messageLogFactory.getMessageLog(message)
   if (log) {
       log.addAttachmentAsString("Debug Info",
           "Processing employee: ${employeeId}", "text/plain")
   }
   ```

4. **Handle errors gracefully:**
   ```groovy
   try {
       def result = processData(input)
   } catch (Exception e) {
       def log = messageLogFactory.getMessageLog(message)
       log.addAttachmentAsString("Error",
           "Failed: ${e.message}", "text/plain")
       throw e
   }
   ```

### Testing

1. **Test in DEV environment first**
2. **Use test flows for end-to-end testing**
3. **Verify in CPI monitoring:**
   - Check message processing logs
   - Verify no errors in runtime
   - Check SuccessFactors for correct updates

4. **Test edge cases:**
   - Empty data
   - Invalid dates/times
   - Missing fields
   - Overlapping vs non-overlapping times

## 📝 Commit Message Format

```
<type>: <short description>

<detailed description>

<footer>
```

**Types:**
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `refactor:` Code refactoring
- `test:` Adding tests
- `chore:` Maintenance tasks

**Example:**
```
fix: Correct timezone handling in conflict analysis

- Changed hardcoded Asia/Riyadh to configurable parameter
- Added timezone validation
- Updated documentation

Resolves: #123
```

## 🔒 Security Guidelines

1. **Never commit credentials**
   - Use CPI secure parameter store
   - Add sensitive files to .gitignore

2. **Review OAuth scopes**
   - Use minimum required permissions
   - Document required scopes in README

3. **Validate all inputs**
   - Check for SQL injection in OData filters
   - Validate date formats
   - Sanitize user inputs

## 📊 Code Review Checklist

Before submitting a pull request:

- [ ] Code follows naming conventions
- [ ] Groovy scripts have header comments
- [ ] Error handling is implemented
- [ ] Logging is added for debugging
- [ ] Documentation is updated
- [ ] Version is incremented in MANIFEST.MF
- [ ] Tested in DEV environment
- [ ] No credentials in code
- [ ] .gitignore is updated if needed
- [ ] Commit messages are descriptive

## 🚀 Deployment Process

1. **Development:**
   - Create/modify flow in DEV
   - Test thoroughly
   - Update documentation

2. **Review:**
   - Create pull request
   - Team review
   - Address feedback

3. **Staging:**
   - Deploy to staging environment
   - Run integration tests
   - Verify with sample data

4. **Production:**
   - Schedule deployment window
   - Deploy to production
   - Monitor for 24 hours
   - Rollback plan ready

## ❓ Questions?

- Check existing documentation in this repository
- Create an issue for questions
- Contact the integration team

## 📞 Support

For urgent issues:
- Email: [integration-team@example.com]
- Slack: #cpi-support
- On-call: [On-call schedule]

---

Thank you for contributing! 🎉
