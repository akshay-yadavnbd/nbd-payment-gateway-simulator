
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

        // Identify only protected admin session requests.
        const isAdminSessionRequest =
            url.startsWith("/api/v1/admin/") &&
            !url.includes("/login");

        // Identify only protected merchant session requests.
        const isMerchantSessionRequest =
            url.startsWith("/api/v1/merchants/") &&
            !url.includes("/login") &&
            !url.includes("/api-key");

        // Redirect only for session authentication failures.
        if (response.status === 401) {

            if (
                window.location.pathname === "/admin.html" &&
                isAdminSessionRequest
            ) {
                window.location.replace(
                    "/admin.html?sessionExpired=true"
                );
            }

            else if (
                window.location.pathname === "/merchant.html" &&
                isMerchantSessionRequest
            ) {
                window.location.replace(
                    "/merchant.html?sessionExpired=true"
                );
            }
        }

        // Preserve backend error messages.
        let errorMessage =
            data.message ||
            data.detail ||
            `Request failed (${response.status})`;

        // Handle backend field validation errors.
        if (data.fields && typeof data.fields === "object") {

            const fieldMessages = Object.values(data.fields)
                .filter(Boolean);

            if (fieldMessages.length > 0) {
                errorMessage = fieldMessages.join("\n");
            }
        }

        throw new Error(errorMessage);
    }

    return data;
}


function showMessage(elementId, message, success = true) {

    const element = document.getElementById(elementId);

    element.textContent = message;
    element.style.whiteSpace = "pre-line";

    element.className =
        "message " + (success ? "success" : "error");
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