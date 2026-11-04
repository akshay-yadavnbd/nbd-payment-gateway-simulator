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

        if (
            response.status === 401 &&
            !url.includes("/login")
        ) {
            const path = window.location.pathname;

            if (path === "/admin.html") {
                window.location.replace(
                    "/admin.html?sessionExpired=true"
                );
            } 
            else if (path === "/merchant.html") {
                window.location.replace(
                    "/merchant.html?sessionExpired=true"
                );
            }
        }

        let errorMessage =
            data.message ||
            data.detail ||
            `Request failed (${response.status})`;

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
