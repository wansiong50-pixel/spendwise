from pathlib import Path
import textwrap

from PIL import Image, ImageDraw, ImageFont, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "docs" / "user-guide"
PAGE_DIR = OUT_DIR / "pdf-pages"
PDF_PATH = OUT_DIR / "SpendWise_New_User_Tutorial.pdf"

W, H = 1275, 1650
M = 90
CONTENT_W = W - 2 * M

INK = (21, 18, 31)
MUTED = (118, 111, 135)
SOFT = (246, 242, 252)
CARD = (255, 255, 255)
LINE = (218, 211, 232)
VIOLET = (105, 79, 255)
VIOLET_DARK = (43, 34, 74)
CORAL = (255, 152, 122)
MINT = (120, 218, 176)
NEG = (235, 87, 110)

FONT_DIR = Path("C:/Windows/Fonts")
REG = FONT_DIR / "arial.ttf"
BOLD = FONT_DIR / "arialbd.ttf"
ITALIC = FONT_DIR / "ariali.ttf"


def font(size, bold=False, italic=False):
    path = BOLD if bold else ITALIC if italic else REG
    return ImageFont.truetype(str(path), size)


F = {
    "kicker": font(22, bold=True),
    "title": font(58, bold=True),
    "subtitle": font(26),
    "h1": font(34, bold=True),
    "h2": font(27, bold=True),
    "body": font(23),
    "body_b": font(23, bold=True),
    "small": font(19),
    "small_b": font(19, bold=True),
    "caption": font(17, italic=True),
    "num": font(22, bold=True),
}


SCREENSHOTS = {
    "home": ROOT / "screenshot_insights_dashboard.png",
    "activity": ROOT / "screenshot_insights_tab_active.png",
    "add": ROOT / "screenshots" / "v2-flow-a-save-visible.png",
    "detail": ROOT / "spendwise-dark-detail-sheet.png",
    "insights": ROOT / "screenshot_insights_picker_open.png",
    "menu": ROOT / "spendwise-dark-insights-menu.png",
}


def wrap(draw, text, fnt, width):
    words = text.split()
    lines = []
    current = ""
    for word in words:
        trial = f"{current} {word}".strip()
        if draw.textlength(trial, font=fnt) <= width:
            current = trial
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def new_page(title=None, section=None, number=1):
    img = Image.new("RGB", (W, H), SOFT)
    draw = ImageDraw.Draw(img)
    if section:
        draw.text((M, 42), section.upper(), fill=VIOLET, font=F["kicker"])
    if title:
        draw.text((M, 82), title, fill=INK, font=F["h1"])
    draw.line((M, H - 64, W - M, H - 64), fill=LINE, width=2)
    draw.text((M, H - 45), "SpendWise New User Tutorial", fill=MUTED, font=F["small"])
    page_txt = str(number)
    draw.text((W - M - draw.textlength(page_txt, font=F["small"]), H - 45), page_txt, fill=MUTED, font=F["small"])
    return img, draw


def paragraph(draw, text, x, y, width=CONTENT_W, fnt=None, fill=INK, gap=8):
    fnt = fnt or F["body"]
    for line in wrap(draw, text, fnt, width):
        draw.text((x, y), line, fill=fill, font=fnt)
        y += fnt.size + gap
    return y + 8


def heading(draw, text, x, y):
    draw.text((x, y), text, fill=INK, font=F["h2"])
    return y + 44


def bullets(draw, items, x, y, width=CONTENT_W, fill=INK):
    for item in items:
        draw.ellipse((x, y + 10, x + 10, y + 20), fill=VIOLET)
        y = paragraph(draw, item, x + 28, y, width - 28, F["body"], fill, 4) + 2
    return y + 6


def steps(draw, items, x, y, width=CONTENT_W):
    for idx, item in enumerate(items, 1):
        draw.rounded_rectangle((x, y - 2, x + 34, y + 32), radius=10, fill=VIOLET_DARK)
        label = str(idx)
        draw.text((x + 17 - draw.textlength(label, font=F["small_b"]) / 2, y + 5), label, fill=(255, 255, 255), font=F["small_b"])
        y = paragraph(draw, item, x + 50, y, width - 50, F["body"], INK, 4) + 4
    return y + 8


def callout(draw, title, body, x, y, width=CONTENT_W, fill=(255, 248, 244), accent=CORAL):
    body_lines = wrap(draw, body, F["body"], width - 46)
    height = 58 + len(body_lines) * 31
    draw.rounded_rectangle((x, y, x + width, y + height), radius=24, fill=fill, outline=LINE, width=2)
    draw.rounded_rectangle((x, y, x + 12, y + height), radius=6, fill=accent)
    draw.text((x + 30, y + 22), title, fill=accent, font=F["body_b"])
    yy = y + 58
    for line in body_lines:
        draw.text((x + 30, yy), line, fill=INK, font=F["body"])
        yy += 31
    return y + height + 24


def table(draw, headers, rows, widths, x, y):
    row_h = 66
    header_h = 58
    total_w = sum(widths)

    wrapped_rows = []
    row_heights = []
    for row in rows:
        wrapped = []
        max_lines = 1
        for i, value in enumerate(row):
            lines = wrap(draw, value, F["small"], widths[i] - 32)
            wrapped.append(lines)
            max_lines = max(max_lines, len(lines))
        wrapped_rows.append(wrapped)
        row_heights.append(max(row_h, 30 * max_lines + 24))

    total_h = header_h + sum(row_heights)
    draw.rounded_rectangle((x, y, x + total_w, y + total_h), radius=18, fill=CARD, outline=LINE, width=2)
    cx = x
    for i, header in enumerate(headers):
        draw.rectangle((cx, y, cx + widths[i], y + header_h), fill=(239, 234, 249))
        draw.text((cx + 18, y + 17), header, fill=INK, font=F["small_b"])
        cx += widths[i]
    yy = y + header_h
    for wrapped, actual_h in zip(wrapped_rows, row_heights):
        cx = x
        draw.line((x, yy, x + total_w, yy), fill=LINE, width=1)
        for i, lines in enumerate(wrapped):
            ty = yy + 14
            for line in lines:
                draw.text((cx + 18, ty), line, fill=INK, font=F["small"])
                ty += 28
            cx += widths[i]
        yy += actual_h
    draw.rounded_rectangle((x, y, x + total_w, y + total_h), radius=18, outline=LINE, width=2)
    return yy + 24


def screenshot(path, target_w, target_h=None):
    with Image.open(path) as im:
        im = im.convert("RGB")
        scale = target_w / im.width
        if target_h and im.height * scale > target_h:
            scale = target_h / im.height
        size = (int(im.width * scale), int(im.height * scale))
        return im.resize(size, Image.Resampling.LANCZOS)


def paste_shot(page, path, x, y, w, caption=None, max_h=None):
    shot = screenshot(path, w, max_h)
    shadow = Image.new("RGBA", (shot.width + 28, shot.height + 28), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.rounded_rectangle((14, 14, 14 + shot.width, 14 + shot.height), radius=34, fill=(25, 15, 45, 55))
    shadow = shadow.filter(ImageFilter.GaussianBlur(10))
    page.paste(shadow, (x - 14, y - 14), shadow)
    mask = Image.new("L", shot.size, 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle((0, 0, shot.width, shot.height), radius=34, fill=255)
    page.paste(shot, (x, y), mask)
    draw = ImageDraw.Draw(page)
    if caption:
        cy = y + shot.height + 12
        for line in wrap(draw, caption, F["caption"], shot.width):
            draw.text((x, cy), line, fill=MUTED, font=F["caption"])
            cy += 24
        return cy + 8
    return y + shot.height


def save_pages(pages):
    PAGE_DIR.mkdir(parents=True, exist_ok=True)
    for old in PAGE_DIR.glob("page-*.png"):
        old.unlink()
    pngs = []
    for i, page in enumerate(pages, 1):
        p = PAGE_DIR / f"page-{i:02d}.png"
        page.save(p)
        pngs.append(p)
    first, rest = pages[0], pages[1:]
    first.save(PDF_PATH, "PDF", resolution=150, save_all=True, append_images=rest)
    return pngs


def page_cover(n):
    img, draw = new_page(number=n)
    draw.text((M, 92), "SPENDWISE", fill=VIOLET, font=F["kicker"])
    draw.text((M, 138), "New User Tutorial", fill=INK, font=F["title"])
    draw.text((M, 206), "& Reference Guide", fill=INK, font=F["title"])
    y = paragraph(
        draw,
        "A detailed beginner guide for recording expenses, reviewing spending, managing accounts and categories, and exporting yearly transaction data.",
        M,
        300,
        555,
        F["subtitle"],
        MUTED,
    )
    y = paragraph(
        draw,
        "Generated 26 May 2026 | Currency: MYR (RM) | Storage: local-first on device",
        M,
        y + 4,
        555,
        F["small"],
        MUTED,
        gap=4,
    )
    callout(draw, "What this app is built for", "SpendWise is designed for quick, private personal tracking: add the transaction, check the month, and move on.", M, y + 10, 560, (255, 248, 244), CORAL)
    paste_shot(img, SCREENSHOTS["home"], 725, 120, 360, max_h=670)
    paste_shot(img, SCREENSHOTS["add"], 820, 805, 300, max_h=650)
    return img


def build_pages():
    pages = []

    img, draw = new_page("What SpendWise Covers", "Overview", 2)
    y = 165
    y = paragraph(draw, "SpendWise is a single-user Android expense tracker. It uses RM for money values and stores your ledger locally on the phone. There is no account sign-in, cloud workspace, collaboration, or social sharing layer.", M, y)
    y = table(draw, ["Area", "What it helps you answer"], [
        ("Home", "How much money do I have, and how is this month going?"),
        ("Activity", "What exactly did I spend or earn this month?"),
        ("Insights", "What is the yearly pattern across income, spend, and categories?"),
        ("Settings", "Where do I manage accounts and categories?"),
    ], [230, 820], M, y + 12)
    y = callout(draw, "Privacy and backup", "Because data is local, export CSV files before uninstalling the app, resetting the phone, or moving records elsewhere.", M, y, accent=VIOLET)
    y = heading(draw, "Quick mental model", M, y)
    bullets(draw, [
        "Add daily details from the plus button.",
        "Read the current month on Home.",
        "Find and correct transactions in Activity.",
        "Use Insights when you want the bigger story.",
    ], M, y)
    pages.append(img)

    img, draw = new_page("First-Time Setup", "Start", 3)
    y = 165
    y = steps(draw, [
        "Open SpendWise. Default categories and a Wallet account are created automatically.",
        "Tap the moon or sun button on Home to choose light or dark mode.",
        "Tap the Total Balance card, or open Settings > Manage accounts, to add your real wallets, bank accounts, e-wallets, or credit accounts.",
        "Use the plus button to add your first expense or income entry.",
        "Return to Home and confirm that balance, heatmap, recent activity, and category totals update.",
    ], M, y)
    y = heading(draw, "Default records", M, y + 12)
    y = paragraph(draw, "Fresh installs include Food, Transport, Bills, Shopping, Health, and Salary. Salary is treated as income; the other built-in categories are treated as expenses. Built-in categories are locked to protect reporting consistency.", M, y)
    callout(draw, "Tip", "Start with broad categories. You can always create custom categories later once your tracking habits settle.", M, y + 10, accent=MINT, fill=(242, 255, 248))
    pages.append(img)

    img, draw = new_page("Home Tab", "Dashboard", 4)
    paste_shot(img, SCREENSHOTS["home"], M, 165, 390, max_h=1180, caption="Home shows total balance, cashflow, heatmap, and category summary.")
    y = 165
    x = 560
    y = heading(draw, "Use Home to scan the month", x, y)
    y = bullets(draw, [
        "Month pill: jump to another tracked month.",
        "Search button: opens Activity search.",
        "Theme button: switches light and dark mode.",
        "Settings button: opens account and category management.",
        "Total Balance card: opens the account list.",
        "Net cashflow: income minus expenses.",
        "Spending heatmap: heavier days appear stronger.",
        "Where you spent: top categories and budget progress.",
        "Recent activity: newest transactions for quick review.",
    ], x, y, 625)
    callout(draw, "When Home looks empty", "Check the selected month first. SpendWise only shows records for the period you are viewing.", x, y, 625, accent=VIOLET)
    pages.append(img)

    img, draw = new_page("Add an Expense or Income", "Transactions", 5)
    paste_shot(img, SCREENSHOTS["add"], 735, 160, 390, max_h=1210, caption="The add sheet captures amount, date, category, account, merchant/source, and notes.")
    y = 165
    y = steps(draw, [
        "Tap the plus button.",
        "Choose Expense or Income.",
        "Enter an amount above RM 0.00.",
        "Tap the date chip if the transaction happened on another day.",
        "Choose category and account.",
        "Enter merchant or source. Merchant is required for expenses and optional for income.",
        "Add notes if helpful.",
        "Tap Save expense or Save income.",
    ], M, y, 570)
    callout(draw, "Amount input", "The amount field accepts digits and one decimal point, with up to two decimal places.", M, y + 4, 570, accent=CORAL)
    pages.append(img)

    img, draw = new_page("Activity, Search, and Details", "Ledger", 6)
    paste_shot(img, SCREENSHOTS["activity"], M, 160, 360, max_h=1120, caption="Activity supports live search and filters.")
    paste_shot(img, SCREENSHOTS["detail"], 815, 160, 360, max_h=1120, caption="Tap a row to share, delete, or edit.")
    y = 1220
    y = bullets(draw, [
        "Search checks merchant, notes, and category names.",
        "Use All, Expense, Income, Account, and Category filters together.",
        "Tap a transaction row to open the detail sheet.",
        "Use Delete for wrong entries, or Edit transaction to correct amount, date, category, account, merchant, or notes.",
    ], M, y, CONTENT_W)
    pages.append(img)

    img, draw = new_page("Accounts", "Money Sources", 7)
    y = 165
    y = paragraph(draw, "Accounts represent where money lives: cash, bank, e-wallet, or credit. SpendWise calculates current balance as starting balance plus income minus expenses assigned to that account.", M, y)
    y = steps(draw, [
        "Open Settings > Manage accounts, or tap the Total Balance card on Home.",
        "Tap Add account.",
        "Enter account name, account type, starting balance, icon, and tile color.",
        "Tap Create account.",
        "To change an account, tap it, update the details, and tap Save changes.",
    ], M, y)
    y = table(draw, ["Account type", "Typical use"], [
        ("Cash", "Physical wallet, cash on hand, petty cash."),
        ("Bank", "Savings or checking account."),
        ("E-wallet", "Touch 'n Go, GrabPay, Boost, or similar wallets."),
        ("Credit", "Credit cards or owed balances."),
    ], [230, 820], M, y + 8)
    callout(draw, "Archiving accounts", "An account can be archived only when no transactions still point to it. Move or edit those transactions first, then archive. Archived accounts can be restored later.", M, y, accent=CORAL)
    pages.append(img)

    img, draw = new_page("Categories and Budgets", "Organization", 8)
    y = 165
    y = paragraph(draw, "Categories drive reporting across Home, Activity, and Insights. Expense categories can also carry optional monthly budgets.", M, y)
    y = steps(draw, [
        "Open Settings > Manage categories, or tap New while adding a transaction.",
        "Choose Expense or Income.",
        "Name the category, choose an icon, and choose a tile color.",
        "For expense categories, optionally enter a monthly budget limit.",
        "Tap Save.",
    ], M, y)
    y = bullets(draw, [
        "Built-in categories are locked.",
        "Custom categories can be edited.",
        "Deleting a custom category with transactions requires a strategy: move those transactions to another category or delete them too.",
        "Budget progress appears in category rows, with nudges around 80 percent and warnings at 100 percent.",
    ], M, y)
    callout(draw, "Naming advice", "Use names you can live with for months. Reports are easier when categories stay broad and stable.", M, y, accent=VIOLET)
    pages.append(img)

    img, draw = new_page("Insights and Export", "Analytics", 9)
    paste_shot(img, SCREENSHOTS["insights"], M, 160, 390, max_h=1100, caption="Insights shows year, net YTD, monthly cashflow, and category split.")
    paste_shot(img, SCREENSHOTS["menu"], 785, 160, 390, max_h=1100, caption="The three-dot menu exports CSV and opens category management.")
    y = 1215
    y = bullets(draw, [
        "Tap Change beside the year to switch years.",
        "Tap a month bar, or use the arrows, to inspect another month.",
        "Use Spend and Income to switch the category donut and list.",
        "Export as CSV includes all transactions for the selected year, regardless of current filters.",
        "PDF export, compare years, and yearly budgets are marked as coming soon in the app.",
    ], M, y)
    pages.append(img)

    img, draw = new_page("Dates, Periods, and Empty States", "Review", 10)
    y = 165
    y = bullets(draw, [
        "Home and Activity use the month picker for monthly review.",
        "Insights uses a year picker plus a selected month inside the cashflow card.",
        "Custom range tools are available when a range sheet opens.",
        "If a list looks empty, clear search text, reset filters to All, and check the selected month or year.",
        "Future months are disabled or visually muted where data cannot exist yet.",
    ], M, y)
    y = callout(draw, "CSV backup habit", "Export a yearly CSV before uninstalling the app, changing phones, or doing any phone reset. CSV export is currently the practical external backup path.", M, y + 10, accent=CORAL)
    y = heading(draw, "What CSV contains", M, y)
    table(draw, ["Column", "Meaning"], [
        ("Date / Time", "When the transaction occurred."),
        ("Type", "Income or Expense."),
        ("Merchant", "Merchant, source, or description."),
        ("Category", "The category name at export time."),
        ("Account", "The account assigned to the transaction."),
        ("Amount / Notes", "Amount in MYR plus optional notes."),
    ], [250, 800], M, y)
    pages.append(img)

    img, draw = new_page("Common Problems", "Troubleshooting", 11)
    y = 165
    table(draw, ["Problem", "What to try"], [
        ("No transactions visible", "Check selected month, clear search, and reset filters to All."),
        ("Cannot save expense", "Enter amount above RM 0.00, choose category/account, and add merchant or description."),
        ("Cannot archive account", "Transactions still point to it. Edit those transactions to another account first."),
        ("Cannot edit built-in category", "Built-in categories are locked. Create a custom category instead."),
        ("CSV says nothing to export", "Switch Insights to a year that has transactions."),
        ("Totals look wrong", "Check whether entries were saved as Income or Expense and assigned to the intended account."),
    ], [360, 690], M, y)
    callout(draw, "Fast sanity check", "If something looks off, inspect the transaction in Activity first. Most issues are an incorrect date, category, account, or type.", M, 1165, accent=VIOLET)
    pages.append(img)

    img, draw = new_page("Good Habits", "Cheat Sheet", 12)
    y = 165
    y = bullets(draw, [
        "Record transactions close to when they happen.",
        "Use accounts consistently so balances remain useful.",
        "Keep category names broad and stable.",
        "Use notes for unusual transactions, reimbursements, or one-off context.",
        "Review Insights monthly to catch patterns before they become background noise.",
        "Export CSV at least once per year or before major phone changes.",
    ], M, y)
    y = callout(draw, "One-page workflow", "Add from plus, verify on Home, find details in Activity, and review longer trends in Insights.", M, y + 8, accent=MINT, fill=(242, 255, 248))
    y = heading(draw, "Main actions", M, y)
    table(draw, ["Need", "Go to"], [
        ("Add expense or income", "Plus button"),
        ("Change theme", "Home moon/sun button"),
        ("Manage accounts", "Home balance card or Settings"),
        ("Manage categories", "Settings or Insights menu"),
        ("Edit/delete transaction", "Activity > transaction detail"),
        ("Export yearly CSV", "Insights > three-dot menu > Export as CSV"),
    ], [330, 720], M, y)
    pages.append(img)

    return [page_cover(1)] + pages


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    pages = build_pages()
    pngs = save_pages(pages)
    print(PDF_PATH)
    print(f"{len(pngs)} pages")


if __name__ == "__main__":
    main()
