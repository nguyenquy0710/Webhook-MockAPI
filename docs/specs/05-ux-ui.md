# UI/UX Design Specifications
## WebHookMock - Webhook Testing Tool

**Version:** 1.0  
**Date:** 2026-04-16  
**Status:** Final  
**Author:** WebHookMock Development Team

---

## 1. Design Principles

### 1.1 Core Principles
- **Simplicity**: Clean, minimal interface requiring no training
- **Consistency**: Uniform design language across all pages
- **Feedback**: Immediate visual feedback for all user actions
- **Accessibility**: WCAG 2.1 Level AA compliant
- **Responsiveness**: Works seamlessly on desktop and tablet devices

### 1.2 Design System
- **Framework**: Bootstrap 5.3
- **Icons**: Font Awesome 6
- **Typography**: System fonts with fallback to Arial, sans-serif
- **Color Palette**: Professional, developer-focused colors
- **Spacing**: 8px base unit system

---

## 2. Color Palette

### 2.1 Primary Colors
| Color | Hex | Usage |
|-------|-----|-------|
| Primary | #0d6efd | Main brand color, primary buttons |
| Secondary | #6c757d | Secondary actions, muted elements |
| Success | #198754 | Success messages, active states |
| Danger | #dc3545 | Error messages, delete actions |
| Warning | #ffc107 | Warning messages, pending states |
| Info | #0dcaf0 | Informational messages |

### 2.2 Background Colors
| Color | Hex | Usage |
|-------|-----|-------|
| Body | #f8f9fa | Page background |
| Card | #ffffff | Content cards |
| Dark | #212529 | Text on light backgrounds |

### 2.3 Text Colors
| Color | Hex | Usage |
|-------|-----|-------|
| Primary | #212529 | Main content text |
| Secondary | #6c757d | Secondary text, metadata |
| Muted | #6c757d | Disabled text, hints |

---

## 3. Typography

### 3.1 Font Sizes
| Element | Size | Weight |
|---------|------|--------|
| H1 | 2.5rem | 700 |
| H2 | 2rem | 700 |
| H3 | 1.75rem | 600 |
| H4 | 1.5rem | 600 |
| H5 | 1.25rem | 600 |
| H6 | 1rem | 600 |
| Body | 1rem | 400 |
| Small | 0.875rem | 400 |
| Micro | 0.75rem | 400 |

### 3.2 Line Heights
| Element | Line Height |
|---------|-------------|
| Headings | 1.2 |
| Body | 1.5 |
| Code | 1.4 |

---

## 4. Component Specifications

### 4.1 Navigation Bar
**Location**: Top of all pages

**Components**:
- Logo (left)
- Navigation links (center)
- User profile dropdown (right)

**States**:
- Active: Primary color background
- Hover: Lightened background
- Mobile: Collapsible hamburger menu

**Responsive**:
- Desktop: Full horizontal navigation
- Tablet: Collapsible menu at 768px
- Mobile: Full-screen overlay menu

### 4.2 Dashboard
**Layout**: 3-column grid

**Cards**:
1. **Quick Actions**
   - Create new webhook
   - View request logs
   - Export data

2. **Statistics**
   - Total webhooks
   - Total requests
   - Recent activity

3. **Recent Webhooks**
   - List of 5 most recent configurations
   - Quick access to edit/delete

**Visual Elements**:
- Cards with shadow depth
- Progress indicators for activity
- Real-time WebSocket updates

### 4.3 Webhook Configuration Form
**Layout**: Single column, stacked form

**Fields**:
1. **Basic Information**
   - Path (text input, required)
   - Method (dropdown, required)
   - Content-Type (dropdown, required)

2. **Response Configuration**
   - Status Code (number input, 100-599)
   - Response Body (textarea, code editor)
   - Response Headers (textarea, JSON editor)

3. **Advanced Options**
   - Delay (slider, 0-60000ms)
   - Preview template variables

**Validation**:
- Real-time field validation
- Error messages below fields
- Success confirmation on save

**Buttons**:
- Primary: Save Configuration
- Secondary: Cancel
- Tertiary: Preview Template

### 4.4 Request Logs Table
**Layout**: Full-width responsive table

**Columns**:
1. ID (numeric, sortable)
2. Method (badge, color-coded)
3. Path (truncated, hover tooltip)
4. Source IP (monospace font)
5. Timestamp (relative + absolute)
6. Response Status (badge)
7. Actions (view, copy curl)

**Features**:
- Pagination (10, 25, 50, 100 per page)
- Search/filter by path
- Real-time updates via WebSocket
- Export to Excel button
- Delete all logs button

**Visual Elements**:
- Color-coded method badges (GET=blue, POST=green, etc.)
- Status code badges (2xx=success, 4xx=warning, 5xx=danger)
- Hover effects on rows
- Sticky header on scroll

### 4.5 Admin Panel
**Layout**: Sidebar navigation + main content

**Sidebar**:
- User management
- System statistics
- Quick actions

**User Management Table**:
- Username, Email, Role, Status, Created At, Actions
- Role toggle (USER/ADMIN)
- Enable/Disable toggle
- Delete user

**Visual Elements**:
- Role badges (ADMIN=primary, USER=secondary)
- Status badges (Active=success, Inactive=danger)
- Confirmation modals for destructive actions

### 4.6 Authentication Pages
**Layout**: Centered card on gradient background

**Components**:
- Logo
- Form fields
- Validation messages
- Links to related pages

**States**:
- Loading: Spinner overlay
- Success: Green confirmation
- Error: Red error banner

---

## 5. Interactive Elements

### 5.1 Buttons
**Sizes**:
- Small: 0.75rem padding
- Default: 0.5rem padding
- Large: 0.75rem padding

**Variants**:
- Primary: Blue background, white text
- Secondary: Gray background, white text
- Outline: Transparent background, border
- Danger: Red background, white text

**States**:
- Hover: Lightened background
- Active: Darkened background
- Disabled: Grayed out, no pointer

### 5.2 Form Inputs
**States**:
- Default: Gray border
- Focus: Primary color border
- Error: Red border
- Success: Green border

**Validation**:
- Inline error messages
- Success icons on valid fields
- Real-time validation feedback

### 5.3 Modals
**Structure**:
- Header (title + close button)
- Body (content)
- Footer (actions)

**Behavior**:
- Click outside to close (optional)
- Escape key to close
- Focus trap for accessibility

### 5.4 Toast Notifications
**Types**:
- Success (green)
- Error (red)
- Warning (yellow)
- Info (blue)

**Behavior**:
- Auto-dismiss after 5 seconds
- Manual close button
- Slide-in animation

---

## 6. Responsive Design

### 6.1 Breakpoints
| Breakpoint | Width | Layout |
|------------|-------|--------|
| xs | < 576px | Single column |
| sm | ≥ 576px | Single column |
| md | ≥ 768px | 2-column grid |
| lg | ≥ 992px | 3-column grid |
| xl | ≥ 1200px | 3-column grid |

### 6.2 Mobile Optimization
- Touch-friendly targets (44px minimum)
- Collapsible navigation
- Stacked forms
- Simplified tables (horizontal scroll)

### 6.3 Tablet Optimization
- 2-column layouts
- Reduced spacing
- Optimized touch targets

---

## 7. Accessibility

### 7.1 Keyboard Navigation
- Full keyboard accessibility
- Focus indicators visible
- Logical tab order
- Skip links for main content

### 7.2 Screen Reader Support
- ARIA labels on interactive elements
- Semantic HTML structure
- Live region updates for dynamic content
- Alt text for all images

### 7.3 Color Contrast
- Text on background: 4.5:1 minimum
- UI elements: 3:1 minimum
- Focus indicators: 3:1 minimum

### 7.4 Error Prevention
- Confirmation dialogs for destructive actions
- Undo capability where possible
- Clear error messages with solutions
- Form validation before submission

---

## 8. Animation and Transitions

### 8.1 Duration
- Short: 150ms (button states)
- Medium: 300ms (modals, drawers)
- Long: 500ms (page transitions)

### 8.2 Easing
- Default: cubic-bezier(0.4, 0, 0.2, 1)
- Entrance: cubic-bezier(0.4, 0, 0.2, 1)
- Exit: cubic-bezier(0.4, 0, 0.2, 1)

### 8.3 Micro-interactions
- Button press: Scale 0.95
- Card hover: Translate Y -2px
- Table row: Background color change
- Form validation: Shake on error

---

## 9. Dark Mode (Future Enhancement)

### 9.1 Color Adjustments
- Background: #1a1a2e
- Cards: #16213e
- Text: #eaeaea
- Borders: #0f3460

### 9.2 Considerations
- Auto-detect system preference
- Manual toggle in user settings
- Smooth transition between modes

---

## 10. Design Files

### 10.1 Assets
- Logo: SVG format
- Icons: Font Awesome CDN
- Images: PNG/JPG with fallback

### 10.2 Style Guide
- CSS variables for colors
- SCSS for component styling
- Custom Bootstrap theme

### 10.3 Component Library
- Reusable Thymeleaf fragments
- Shared CSS classes
- JavaScript utilities

---

*Document Version: 1.0*  
*Last Updated: 2026-04-16*
