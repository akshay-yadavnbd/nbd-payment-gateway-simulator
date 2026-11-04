async function api(url, options = {}) {
    const response = await fetch(url, {
        credentials: "same-origin",
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        }
    });

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
        if (response.status === 401) {

            const path = window.location.pathname;

            if (url.includes("/api/v1/admin/") &&
                !url.includes("/login")) {

                window.location.replace(
                    "/admin.html?sessionExpired=true"
                );

            } else if (url.includes("/api/v1/merchants/") &&
                    !url.includes("/login")) {

                window.location.replace(
                    "/merchant.html?sessionExpired=true"
                );
            }

            throw new Error(
                "Your session has expired. Please log in again."
            );
        }
    }

    return data;
}

function showMessage(elementId, message, success = true) {
    const element = document.getElementById(elementId);
    element.textContent = message;
    element.style.whiteSpace = "pre-line";
    element.className = "message " + (success ? "success" : "error");
}

async function getCsrfToken(type) {
    const data = await api(`/api/v1/${type}/csrf`);

    return {
        [data.headerName]: data.token
    };
}

function formatAmount(paise) {
    return "₹" + (Number(paise) / 100).toFixed(2);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}
