# Security

## Reporting vulnerabilities

If you find a security issue, please report it responsibly:

1. Open a [GitHub Security Advisory](https://github.com/Noelithub77/wifix2/security/advisories/new) (preferred), or
2. Email the maintainers directly (see repository contacts)

Do not open public issues for security vulnerabilities.

## What to report

- Credential leakage or insecure storage
- Network traffic interception or MITM risks
- Privilege escalation or unintended permission use
- Other issues that could harm users or their data

## Response

We aim to acknowledge reports within 48 hours and provide an initial assessment within 7 days.

## Security considerations

- **Credentials**: Username and password are stored in app-private SharedPreferences. They are not encrypted at rest beyond Android’s default app sandbox.
- **Network**: Login uses HTTP (not HTTPS) to captive portal endpoints. Traffic stays on the local network; credentials are not sent to external servers.
- **Permissions**: The app requests only the permissions listed in the README and uses them for the described functionality.
