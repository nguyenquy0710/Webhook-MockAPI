# UI/UX standards

Source of truth: prototype screen `blank` (`docs/prototype/_gen/frag/blank.html`). Theme: `bootstrap` 5.3.3 (https://getbootstrap.com/).

## Colors
| Hex | Token | Used for |
|---|---|---|
| #0D6EFD | --bs-primary | Primary buttons, links, active nav pill |
| #198754 | --bs-success | Success badges and alerts, 2xx status |
| #DC3545 | --bs-danger | Errors, delete, 4xx/5xx status |
| #FFC107 | --bs-warning | Warnings, 3xx status, disabled account |
| #0DCAF0 | --bs-info | Info badges, PUT method, log actions |
| #6C757D | --bs-secondary | Secondary buttons and badges |
| #212529 | --bs-dark | Header background, table head |
| #F8F9FA | --bs-light | Page background, code blocks |

## Typography, spacing, shape
System UI font stack, 16px base; monospace 0.875rem for code and logs. Bootstrap spacing scale. Card radius 0.375rem, auth cards `rounded-4`, pills `rounded-pill`. Cards `shadow-sm`, header `shadow`.

## Conventions
- English UI; dates `yyyy-MM-dd HH:mm:ss` (UTC for logs); status codes as badges.
- Icons: Font Awesome 6.5.1. Destructive actions ask for confirmation.
- Empty table: muted inbox icon plus hint. Success/error: dismissible alerts or toasts.
- Labels on all fields, `aria-label`/tooltip on icon-only buttons.
- Sample data only (user `demo`, domain `mock.example.test`).
