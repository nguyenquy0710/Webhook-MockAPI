// Show/hide password buttons: <button data-toggle-password="<input id>">
document.addEventListener('click', function (e) {
    var btn = e.target.closest('[data-toggle-password]');
    if (!btn) return;
    var input = document.getElementById(btn.getAttribute('data-toggle-password'));
    var icon = btn.querySelector('i');
    var show = input.type === 'password';
    input.type = show ? 'text' : 'password';
    if (icon) icon.className = 'fas ' + (show ? 'fa-eye-slash' : 'fa-eye');
    btn.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
});
