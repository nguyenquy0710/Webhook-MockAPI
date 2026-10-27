#!/usr/bin/env python3
"""Dựng HTML hoàn chỉnh cho từng màn prototype: ghép bố cục với hành vi tương tác.

Cách dùng (chạy từ thư mục nào cũng được):
    python _gen/interact.py <màn> [<màn> ...]   dựng các màn chỉ định
    python _gen/interact.py --all               dựng mọi màn có trong _gen/orig và _gen/frag
    python _gen/interact.py --all --strict      như trên, thoát mã 1 nếu còn cảnh báo (dùng làm cổng trước khi publish)
    python _gen/interact.py --changed           in các file project/*.dc.html khác với bản đã publish
    python _gen/interact.py --record            ghi băm các file hiện có vào .artifact.json (gọi sau khi publish xong)

Nguồn:   _gen/orig/<màn>.dc.html (ưu tiên)  hoặc  _gen/frag/<màn>.html (ghép vào _gen/frame.html)
Đầu ra:  project/<màn>.dc.html. Đừng sửa tay: sửa nguồn trong _gen/ rồi dựng lại.

Bộ dựng làm 4 việc:
  1. Bọc vùng ẩn/hiện:    <!--region:tên--> ... <!--/region-->  (thêm "hidden" sau tên để ẩn mặc định)
  2. Gắn hành vi vào nút theo chữ hiển thị (actions.py). Nhãn nút phải khớp khóa trong actions.py.
  3. Nút thao tác trong bảng -> icon (icons.py), giữ nhãn ở title/aria-label.
  4. Chèn mã chạy tương tác (hộp thoại, ô nhập, bật/tắt vùng, thông báo, mô phỏng phản hồi).

Chỉ dùng thư viện chuẩn của Python 3.
"""
import hashlib
import html
import json
import re
import sys
import unicodedata
from pathlib import Path

sys.dont_write_bytecode = True
GEN = Path(__file__).resolve().parent
ROOT = GEN.parent
OUT = ROOT / "project"
META = ROOT / ".artifact.json"
sys.path.insert(0, str(GEN))

import actions as actions_mod  # noqa: E402
import build as build_mod  # noqa: E402
import icons as icons_mod  # noqa: E402

BEGIN, END = "<!--proto:begin-->", "<!--proto:end-->"
TYPES = {"modal", "confirm", "toggle", "toast", "goto", "close"}
STRING_DEFAULTS = {
    "required": "Vui lòng nhập {label}", "processing": "Đang xử lý…", "saved": "Đã lưu",
    "cancel": "Hủy", "confirm": "Lưu", "confirm_danger": "Xóa",
    "error": "Không thực hiện được. Vui lòng thử lại.",
}

RUNTIME_CSS = """
[hidden]{display:none!important}
.icon-btn{display:inline-flex;align-items:center;justify-content:center;padding:4px;line-height:1;cursor:pointer}
.icon-btn svg{width:16px;height:16px;fill:none;stroke:currentColor;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}
.proto-layer{position:fixed;inset:0;background:rgba(0,0,0,.45);display:flex;align-items:center;justify-content:center;padding:16px;z-index:2147483000}
.proto-dialog{background:var(--proto-surface,Canvas);color:var(--proto-text,CanvasText);border-radius:12px;padding:20px;width:min(440px,100%);max-height:90vh;overflow:auto;box-shadow:0 20px 50px rgba(0,0,0,.3);font:inherit}
.proto-title{margin:0 0 8px;font-size:1.1em}
.proto-body{margin:0 0 12px;opacity:.8}
.proto-field{margin:0 0 12px;display:flex;flex-direction:column;gap:4px}
.proto-field input,.proto-field select,.proto-field textarea{font:inherit;padding:8px;border:1px solid rgba(128,128,128,.5);border-radius:8px;background:transparent;color:inherit}
.proto-field [aria-invalid=true]{border-color:#dc2626}
.proto-error{color:#dc2626;font-size:.9em}
.proto-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:16px}
.proto-btn{font:inherit;padding:8px 14px;border-radius:8px;border:1px solid rgba(128,128,128,.5);background:transparent;color:inherit;cursor:pointer}
.proto-btn-primary{background:var(--proto-accent,#2563eb);border-color:var(--proto-accent,#2563eb);color:#fff}
.proto-btn-danger{background:#dc2626;border-color:#dc2626}
.proto-btn:disabled{opacity:.6;cursor:default}
.proto-toasts{position:fixed;right:16px;bottom:16px;display:flex;flex-direction:column;gap:8px;z-index:2147483001}
.proto-toast{background:#111827;color:#fff;padding:10px 14px;border-radius:8px;box-shadow:0 8px 24px rgba(0,0,0,.3)}
.proto-toast-error{background:#b91c1c}
"""

RUNTIME_JS = r"""
(function () {
  var A = window.__PROTO_ACTIONS || {};
  var S = window.__PROTO_STRINGS || {};
  var layer = null, opener = null;
  function el(tag, cls, text) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text != null) e.textContent = text;
    return e;
  }
  function toast(msg, kind) {
    var box = document.querySelector('.proto-toasts');
    if (!box) {
      box = el('div', 'proto-toasts');
      box.setAttribute('role', 'status');
      box.setAttribute('aria-live', 'polite');
      document.body.appendChild(box);
    }
    var t = el('div', 'proto-toast' + (kind === 'error' ? ' proto-toast-error' : ''), msg);
    box.appendChild(t);
    setTimeout(function () { if (t.parentNode) t.parentNode.removeChild(t); }, 3200);
  }
  function closeModal() {
    if (layer && layer.parentNode) layer.parentNode.removeChild(layer);
    layer = null;
    if (opener && opener.focus) opener.focus();
    opener = null;
  }
  function openModal(spec, danger) {
    closeModal();
    opener = document.activeElement;
    layer = el('div', 'proto-layer');
    var dlg = el('div', 'proto-dialog');
    dlg.setAttribute('role', 'dialog');
    dlg.setAttribute('aria-modal', 'true');
    dlg.setAttribute('aria-labelledby', 'proto-title');
    var title = el('h2', 'proto-title', spec.title || '');
    title.id = 'proto-title';
    dlg.appendChild(title);
    if (spec.body) dlg.appendChild(el('p', 'proto-body', spec.body));
    var inputs = [];
    (spec.fields || []).forEach(function (f, i) {
      var row = el('div', 'proto-field');
      var id = 'proto-f' + i;
      var lab = el('label', null, f.label || '');
      lab.setAttribute('for', id);
      var ctl;
      if (f.type === 'select') {
        ctl = el('select');
        (f.options || []).forEach(function (o) { var op = el('option', null, o); op.value = o; ctl.appendChild(op); });
      } else if (f.type === 'textarea') {
        ctl = el('textarea');
        ctl.rows = 3;
      } else {
        ctl = el('input');
        ctl.type = f.type || 'text';
      }
      ctl.id = id;
      if (f.placeholder) ctl.placeholder = f.placeholder;
      if (f.value != null) ctl.value = f.value;
      var err = el('div', 'proto-error');
      err.hidden = true;
      row.appendChild(lab); row.appendChild(ctl); row.appendChild(err);
      dlg.appendChild(row);
      inputs.push({ f: f, ctl: ctl, err: err });
    });
    var bar = el('div', 'proto-actions');
    var cancel = el('button', 'proto-btn', spec.cancel || S.cancel);
    cancel.type = 'button';
    var ok = el('button', 'proto-btn proto-btn-primary' + (danger ? ' proto-btn-danger' : ''),
                spec.confirm || (danger ? S.confirm_danger : S.confirm));
    ok.type = 'button';
    bar.appendChild(cancel); bar.appendChild(ok); dlg.appendChild(bar);
    layer.appendChild(dlg);
    document.body.appendChild(layer);
    cancel.addEventListener('click', closeModal);
    layer.addEventListener('mousedown', function (e) { if (e.target === layer) closeModal(); });
    ok.addEventListener('click', function () {
      var valid = true, first = null;
      inputs.forEach(function (x) {
        var empty = !String(x.ctl.value).trim();
        if (x.f.required && empty) {
          x.err.textContent = (S.required || '').replace('{label}', String(x.f.label || '').toLowerCase());
          x.err.hidden = false;
          x.ctl.setAttribute('aria-invalid', 'true');
          valid = false;
          if (!first) first = x.ctl;
        } else {
          x.err.hidden = true;
          x.ctl.removeAttribute('aria-invalid');
        }
      });
      if (!valid) { first.focus(); return; }
      var label = ok.textContent;
      ok.disabled = true; cancel.disabled = true; ok.textContent = S.processing;
      setTimeout(function () {
        if (spec.outcome === 'error') {
          ok.disabled = false; cancel.disabled = false; ok.textContent = label;
          toast(spec.error || S.error, 'error');
          return;
        }
        closeModal();
        toast(spec.toast || S.saved);
      }, spec.delay == null ? 600 : spec.delay);
    });
    (inputs.length ? inputs[0].ctl : ok).focus();
  }
  function toggle(name, trigger) {
    var r = document.querySelector('[data-region="' + name + '"]');
    if (!r) return;
    r.hidden = !r.hidden;
    if (trigger && trigger.setAttribute) trigger.setAttribute('aria-expanded', String(!r.hidden));
  }
  function run(spec, trigger) {
    switch (spec.type) {
      case 'modal': openModal(spec, false); break;
      case 'confirm': openModal(spec, true); break;
      case 'toggle': toggle(spec.region, trigger); break;
      case 'toast':
        if (spec.delay) {
          var old = trigger.innerHTML;
          trigger.disabled = true;
          trigger.textContent = S.processing;
          setTimeout(function () {
            trigger.disabled = false;
            trigger.innerHTML = old;
            toast(spec.message, spec.outcome === 'error' ? 'error' : null);
          }, spec.delay);
        } else {
          toast(spec.message, spec.outcome === 'error' ? 'error' : null);
        }
        break;
      case 'goto':
        var ev = new CustomEvent('proto:goto', { detail: { screen: spec.screen }, cancelable: true });
        if (window.dispatchEvent(ev)) location.href = spec.screen + '.dc.html';
        break;
      case 'close': closeModal(); break;
    }
  }
  document.addEventListener('click', function (e) {
    var t = e.target.closest ? e.target.closest('[data-act]') : null;
    if (!t || t.disabled) return;
    var spec = A[t.getAttribute('data-act')];
    if (!spec) return;
    e.preventDefault();
    run(spec, t);
  });
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape') closeModal(); });
})();
"""


# ---------- tiện ích ----------

def norm(text):
    """Chuẩn hóa nhãn: bỏ thẻ, giải mã ký tự HTML, NFC, gộp khoảng trắng, chữ thường."""
    text = re.sub(r"<[^>]+>", " ", text)
    text = unicodedata.normalize("NFC", html.unescape(text))
    return re.sub(r"\s+", " ", text).strip().lower()


def get_attr(attrs, name):
    m = re.search(r"\s%s\s*=\s*(?:\"([^\"]*)\"|'([^']*)')" % re.escape(name), attrs, re.I)
    if not m:
        return None
    return html.unescape(m.group(1) if m.group(1) is not None else m.group(2))


def set_attr(attrs, name, value):
    pat = re.compile(r"\s%s\s*=\s*(?:\"[^\"]*\"|'[^']*')" % re.escape(name), re.I)
    new = ' %s="%s"' % (name, html.escape(value, quote=True))
    if pat.search(attrs):
        return pat.sub(lambda _m: new, attrs, count=1)
    return attrs.rstrip() + new


def add_class(attrs, cls):
    cur = get_attr(attrs, "class")
    return set_attr(attrs, "class", (cur + " " + cls) if cur else cls)


def load_actions(name):
    merged = {}
    scoped = {}
    for scope in ("*", name):
        for label, spec in actions_mod.ACTIONS.get(scope, {}).items():
            merged[norm(label)] = spec
            if scope == name:
                scoped[norm(label)] = spec
    return merged, scoped


def strings():
    out = dict(STRING_DEFAULTS)
    out.update(getattr(actions_mod, "STRINGS", {}))
    return out


def screen_names():
    names = set()
    for sub, suffix in (("orig", ".dc.html"), ("frag", ".html")):
        d = GEN / sub
        if d.is_dir():
            for p in d.glob("*" + suffix):
                names.add(p.name[: -len(suffix)])
    return names


def source_html(name):
    orig = GEN / "orig" / (name + ".dc.html")
    if orig.is_file():
        return orig.read_text(encoding="utf-8")
    if (GEN / "frag" / (name + ".html")).is_file():
        return build_mod.render(name)
    raise FileNotFoundError("Không có nguồn cho màn '%s' (tìm _gen/orig/%s.dc.html hoặc _gen/frag/%s.html)" % (name, name, name))


# ---------- dựng một màn ----------

CLICKABLE = re.compile(r"<(button|a)\b([^>]*)>(.*?)</\1\s*>", re.I | re.S)
REGION = re.compile(r"<!--\s*region:([\w-]+)(\s+hidden)?\s*-->(.*?)<!--\s*/region\s*-->", re.S)


def is_clickable_anchor(attrs):
    return bool(re.search(r"class\s*=\s*[\"'][^\"']*\bbtn\b", attrs, re.I) or re.search(r"role\s*=\s*[\"']button", attrs, re.I))


def build_screen(name, known_screens):
    warnings = []
    text = source_html(name)

    # bỏ khối dựng cũ nếu nguồn lỡ chứa (để dựng lại không bị nhân đôi)
    text = re.sub(re.escape(BEGIN) + r".*?" + re.escape(END), "", text, flags=re.S)

    # 1. bọc vùng ẩn/hiện
    regions = set(re.findall(r"data-region\s*=\s*[\"']([\w-]+)[\"']", text))

    def wrap(m):
        regions.add(m.group(1))
        return '<div data-region="%s"%s>%s</div>' % (m.group(1), " hidden" if m.group(2) else "", m.group(3))

    text = REGION.sub(wrap, text)

    merged, scoped = load_actions(name)
    icons = {norm(k): v for k, v in icons_mod.ICONS.items()}
    td_spans = [(m.start(), m.end()) for m in re.finditer(r"<td\b.*?</td\s*>", text, re.I | re.S)]
    used = set()
    payload = {}
    counters = {"buttons": 0, "icons": 0}
    unmapped = []

    def in_td(pos):
        return any(s <= pos < e for s, e in td_spans)

    def handle(m):
        tag, attrs, inner = m.group(1), m.group(2), m.group(3)
        if tag.lower() == "a" and not is_clickable_anchor(attrs):
            return m.group(0)
        if re.search(r"\sdisabled\b", attrs, re.I) or get_attr(attrs, "data-proto") == "skip":
            return m.group(0)
        # Bootstrap JS tự xử lý modal/collapse/dropdown/tab; liên kết có href thật chỉ là điều hướng, không cần hành vi
        if get_attr(attrs, "data-bs-toggle") in ("modal", "collapse", "dropdown", "tab") or get_attr(attrs, "data-bs-dismiss"):
            return m.group(0)
        if tag.lower() == "a" and (get_attr(attrs, "href") or "#") != "#":
            return m.group(0)
        shown = re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", " ", inner))).strip()
        label = norm(inner) or norm(get_attr(attrs, "aria-label") or get_attr(attrs, "title") or "")
        key = norm(get_attr(attrs, "data-act") or "") or label
        if not key:
            return m.group(0)
        as_icon = in_td(m.start()) and label in icons
        if key in merged:
            spec = merged[key]
            used.add(key)
            aid = "a%d" % (len(payload) + 1)
            payload[aid] = spec
            attrs = set_attr(attrs, "data-act", aid)
            counters["buttons"] += 1
        else:
            unmapped.append(shown or label)
        if as_icon:
            attrs = add_class(attrs, "icon-btn")
            attrs = set_attr(attrs, "title", get_attr(attrs, "title") or shown)
            attrs = set_attr(attrs, "aria-label", get_attr(attrs, "aria-label") or shown)
            inner = icons[label]
            counters["icons"] += 1
        return "<%s%s>%s</%s>" % (tag, attrs, inner, tag)

    text = CLICKABLE.sub(handle, text)

    # cảnh báo
    for label in sorted(set(unmapped)):
        warnings.append("[%s] nút '%s' chưa có hành vi trong actions.py" % (name, label))
    for key in sorted(set(scoped) - used):
        warnings.append("[%s] hành vi '%s' khai báo nhưng không khớp nút nào (đổi tên nút?)" % (name, key))
    for aid, spec in payload.items():
        t = spec.get("type")
        if t not in TYPES:
            warnings.append("[%s] hành vi có loại không hợp lệ: %r (hợp lệ: %s)" % (name, t, ", ".join(sorted(TYPES))))
        elif t == "toggle" and spec.get("region") not in regions:
            warnings.append("[%s] hành vi toggle trỏ tới vùng '%s' không có trong nguồn" % (name, spec.get("region")))
        elif t == "goto" and spec.get("screen") not in known_screens:
            warnings.append("[%s] hành vi goto trỏ tới màn '%s' chưa có nguồn" % (name, spec.get("screen")))

    # 4. chèn mã tương tác
    def js_json(obj):
        return json.dumps(obj, ensure_ascii=False).replace("</", "<\\/")

    block = "\n".join([
        BEGIN,
        "<!-- Được sinh bởi _gen/interact.py. Đừng sửa tay: sửa nguồn trong _gen/ rồi dựng lại. -->",
        "<style>" + RUNTIME_CSS + "</style>",
        "<script>window.__PROTO_ACTIONS=" + js_json(payload) + ";window.__PROTO_STRINGS=" + js_json(strings()) + ";</script>",
        "<script>" + RUNTIME_JS + "</script>",
        END,
    ])
    idx = text.lower().rfind("</body>")
    text = (text[:idx] + block + "\n" + text[idx:]) if idx != -1 else (text.rstrip() + "\n" + block + "\n")

    OUT.mkdir(parents=True, exist_ok=True)
    out = OUT / (name + ".dc.html")
    out.write_text(text, encoding="utf-8", newline="\n")
    print("OK  %s -> %s  (%d nút gắn hành vi, %d icon, %d vùng ẩn/hiện)" % (
        name, out.relative_to(ROOT).as_posix(), counters["buttons"], counters["icons"], len(regions)))
    return warnings


# ---------- phát hành từng phần ----------

def sha(path):
    return hashlib.sha1(path.read_bytes()).hexdigest()


def load_meta():
    try:
        return json.loads(META.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}


def cmd_changed():
    published = load_meta().get("published", {})
    for p in sorted(OUT.glob("*.dc.html")):
        if published.get(p.name) != sha(p):
            print(p.relative_to(ROOT).as_posix())


def cmd_record():
    meta = load_meta()
    meta["published"] = {p.name: sha(p) for p in sorted(OUT.glob("*.dc.html"))}
    META.write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Đã ghi băm %d file vào %s" % (len(meta["published"]), META.name))


def canvas_warnings(names):
    canvas = OUT / "canvas.json"
    if not canvas.is_file():
        return ["chưa có project/canvas.json: màn hình chưa được khai báo vào canvas"]
    raw = canvas.read_text(encoding="utf-8")
    return ["[%s] chưa thấy khai báo trong project/canvas.json (thêm file HTML chưa đủ để thêm màn vào canvas)" % n
            for n in names if n not in raw]


def main(argv):
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    args = [a for a in argv if not a.startswith("--")]
    flags = {a for a in argv if a.startswith("--")}
    if "--changed" in flags:
        return cmd_changed() or 0
    if "--record" in flags:
        return cmd_record() or 0
    known = screen_names()
    if "--all" in flags:
        args = sorted(known)
    if not args:
        print(__doc__)
        return 2
    warnings = []
    failed = False
    for name in args:
        try:
            warnings += build_screen(name, known)
        except FileNotFoundError as exc:
            print("LỖI " + str(exc))
            failed = True
    if not failed:
        warnings += canvas_warnings(args)
    for w in warnings:
        print("CẢNH BÁO " + w)
    if failed:
        return 1
    return 1 if (warnings and "--strict" in flags) else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
