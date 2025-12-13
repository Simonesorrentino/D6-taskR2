/*
 * Copyright (c) 2025 Stefano Marano
 * Versione: Bridge Thymeleaf -> JS
 */

// === CONFIGURAZIONE URL ===
const links = {
    navDashboardAdmin: VIEWS.DASHBOARD_ADMIN,
    navHintsMain: VIEWS.HINTS_MAIN,
    navDashboardAdmin2: VIEWS.DASHBOARD_ADMIN,
    navHintsUpload: VIEWS.HINTS_UPLOAD,
};
assignUrls(links);

// === FUNZIONE TRADUZIONE (Legge dal serverMessages iniettato nell'HTML) ===
function t(key, ...args) {
    // Controlla se l'oggetto esiste (iniettato da Thymeleaf)
    if (typeof serverMessages === 'undefined') {
        console.error("Errore: serverMessages non definito. Controlla hints_main.html");
        return key;
    }

    let text = serverMessages[key] || key;

    // Sostituzione placeholder {0}, {1}...
    args.forEach((arg, index) => {
        text = text.replace(`{${index}}`, arg);
    });

    return text;
}


// === VARIABILI GLOBALI ===
let currentActiveHint = null;

// === FUNZIONI DI UTILITY ===

function escapeHtml(unsafe) {
    if (!unsafe) return '';
    return unsafe.toString().replace(/&/g, "&amp;")
                 .replace(/</g, "&lt;")
                 .replace(/>/g, "&gt;")
                 .replace(/"/g, "&quot;")
                 .replace(/'/g, "&#039;")
                 .replace(/`/g, "&#x60;");
}

function toggleInitialView(show) {
    const initialButtons = document.getElementById('initial-buttons');
    if (initialButtons) initialButtons.style.display = show ? 'flex' : 'none';

    const navbarContent = document.getElementById('navbarSupportedContent');
    if (navbarContent) navbarContent.style.display = show ? 'block' : 'none';
}

function setDynamicContent(html) {
    document.getElementById('dynamic-content').innerHTML = html;
}

function getActionButtons(hintId, isFirst) {
    return `
        <div class="btn-group-vertical btn-group-sm">
            <button onclick="event.stopPropagation(); moveHintJS(${hintId}, 'UP')"
                    class="btn btn-outline-secondary" ${isFirst ? 'disabled' : ''} title="UP">
                <i class="fa fa-chevron-up"></i>
            </button>
            <button onclick="event.stopPropagation(); moveHintJS(${hintId}, 'DOWN')"
                    class="btn btn-outline-secondary" title="DOWN">
                <i class="fa fa-chevron-down"></i>
            </button>
        </div>
    `;
}

// === FUNZIONI DI ELIMINAZIONE ===

async function deleteHintByClassUTAndOrder(classUTName, order) {
    if (!confirm(t('msg_confirm_delete_single'))) return;

    const safeClassUTName = classUTName === 'null' ? 'null' : encodeURIComponent(classUTName);
    const url = `${APIS.DELETE_HINT_BY_CLASS_AND_ORDER}/${safeClassUTName}/order/${order}`;
    await callDeleteHint(url);
}

async function deleteAllHintsForSpecificClass(classUTName) {
    if (!confirm(t('msg_confirm_delete_specific', classUTName))) return;

    const safeClassUTName = encodeURIComponent(classUTName);
    const url = `${APIS.DELETE_HINT_BY_CLASS}/${safeClassUTName}`;
    await callDeleteHint(url);
}

async function deleteAllClassHints() {
    if (!confirm(t('msg_confirm_delete_all_class'))) return;

    const url = `${APIS.DELETE_HINT_BY_TYPE}/CLASS`;
    await callDeleteHint(url);
}

async function deleteAllGenericHints() {
    if (!confirm(t('msg_confirm_delete_all_generic'))) return;

    const url = `${APIS.DELETE_HINT_BY_CLASS}/null`;
    await callDeleteHint(url);
}

async function callDeleteHint(url) {
    try {
        await redirectOnSuccessTemplate({
            url: url,
            method: "DELETE",
            headers: { 'Content-Type': 'application/json' },
        },
        { reload: true });
    } catch (error) {
        let errorMessage = t('err_unknown');
        if (error.response && error.response.message) errorMessage = error.response.message;
        else if (error.message) errorMessage = error.message;

        displayError(errorMessage);
    }
}

// === API MOVEMENT ===
async function moveHintJS(id, direction) {
    const url = `/hints/${id}/move?direction=${direction}`;
    const token = getCookie("jwtToken");

    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Authorization': 'Bearer ' + token }
        });

        if (response.ok) {
            if (document.getElementById('genericTable')) {
                renderGenericHintsView();
            } else {
                const titleEl = document.querySelector('h2');
                if (titleEl) {
                     const classNameMatch = titleEl.innerText.replace(t('title_class_detail', ''), '').trim();
                     if(classNameMatch) {
                         renderClassHintsDetailView(classNameMatch);
                     } else {
                         location.reload();
                     }
                }
            }
        } else {
            console.error("Errore spostamento");
        }
    } catch (e) {
        console.error("Errore rete", e);
    }
}


// === GESTIONE DELLA RICERCA ===
function handleSearchSubmit(event) {
    event.preventDefault();
    document.location.reload();
}

// === VISTE DI RENDERING ===

// --- VIEW 1: Elenco Suggerimenti Generici ---
async function renderGenericHintsView() {
    toggleInitialView(false);
    setDynamicContent(`<p class="text-info">${t('loading_generic')}</p>`);

    try {
        const hints = await callGetHints("type=GENERIC");

        const headerButtons = `
            <div class="d-flex justify-content-between align-items-center mb-3">
                <button onclick="document.location.reload()" class="btn btn-sm btn-secondary">
                    <i class="fa fa-arrow-left"></i> ${t('back_selection')}
                </button>
                ${(hints && hints.length > 0) ? `
                <button onclick="deleteAllGenericHints()" class="btn btn-sm btn-danger">
                    <i class="fa fa-trash"></i> ${t('btn_delete_all_generic')}
                </button>` : ''}
            </div>
        `;

        if (!hints || hints.length === 0) {
            setDynamicContent(`
                <h2>${t('title_generic')}</h2>
                ${headerButtons}
                <p class="text-warning">${t('msg_no_generic')}</p>
            `);
            return;
        }

        hints.sort((a, b) => a.order - b.order);

        let tableHtml = `
            <h2>${t('title_generic')}</h2>
            ${headerButtons}
            <table id="genericTable" class="table table-hover table-striped">
                <thead>
                    <tr>
                        <th style="width: 5%;">${t('col_actions')}</th>
                        <th style="width: 5%;">${t('col_order')}</th>
                        <th style="width: 20%;">${t('col_title')}</th>
                        <th style="width: 55%;">${t('col_content')}</th>
                        <th style="width: 15%;">${t('col_type')}</th>
                    </tr>
                </thead>
                <tbody>
        `;

        for (let i = 0; i < hints.length; i++) {
            const hint = hints[i];
            const safeName = escapeHtml(hint.name);
            const safeContentSnippet = escapeHtml(hint.content).substring(0, 100) + (hint.content.length > 100 ? '...' : '');
            const actionButtons = getActionButtons(hint.id, i === 0);

            tableHtml += `
                <tr onclick="renderHintDetailView('${hint.order}', 'GENERIC')" style="cursor:pointer;">
                    <td>${actionButtons}</td>
                    <td>${hint.order}</td>
                    <td><strong>${safeName}</strong></td>
                    <td>${safeContentSnippet}</td>
                    <td><span class="badge badge-info">${hint.type}</span></td>
                </tr>
            `;
        }

        tableHtml += `</tbody></table>`;
        setDynamicContent(tableHtml);

    } catch (error) {
        setDynamicContent(`<p class="text-danger">${t('msg_error_fetch', error.message)}</p>`);
    }
}

// --- VIEW 2: Elenco Classi ---
async function renderClassHintsListView() {
    toggleInitialView(false);
    setDynamicContent(`<p class="text-info">${t('loading_classes')}</p>`);

    try {
        const hints = await callGetHints("type=CLASS");

        const headerButtons = `
            <div class="d-flex justify-content-between align-items-center mb-3">
                <button onclick="document.location.reload()" class="btn btn-sm btn-secondary">
                    <i class="fa fa-arrow-left"></i> ${t('back_selection')}
                </button>
                ${(hints && hints.length > 0) ? `
                <button onclick="deleteAllClassHints()" class="btn btn-sm btn-danger">
                    <i class="fa fa-trash"></i> ${t('btn_delete_all_class')}
                </button>` : ''}
            </div>
        `;

        if (!hints || hints.length === 0) {
            setDynamicContent(`
                <h2>${t('title_classes')}</h2>
                ${headerButtons}
                <p class="text-warning">${t('msg_no_classes')}</p>
            `);
            return;
        }

        const classGroup = hints.reduce((acc, hint) => {
            const className = hint.classUTName;
            if (className) acc[className] = (acc[className] || 0) + 1;
            return acc;
        }, {});

        const sortedClassNames = Object.keys(classGroup).sort();

        let tableHtml = `
            <h2>${t('title_classes')}</h2>
            ${headerButtons}
            <table class="table table-hover table-striped">
                <thead>
                    <tr>
                        <th style="width: 70%;">${t('col_class_name')}</th>
                        <th style="width: 30%;">${t('col_hint_count')}</th>
                    </tr>
                </thead>
                <tbody>
        `;

        for (const className of sortedClassNames) {
            const safeClassName = escapeHtml(className);
            const count = classGroup[className];

            tableHtml += `
                <tr onclick="renderClassHintsDetailView('${safeClassName}')" style="cursor:pointer;">
                    <td>${safeClassName}</td>
                    <td>${count}</td>
                </tr>
            `;
        }

        tableHtml += `</tbody></table>`;
        setDynamicContent(tableHtml);

    } catch (error) {
        setDynamicContent(`<p class="text-danger">${t('msg_error_fetch', error.message)}</p>`);
    }
}

// --- VIEW 2.1: Elenco Suggerimenti Classe Specifica ---
async function renderClassHintsDetailView(classUTName) {
    toggleInitialView(false);
    const safeClassName = escapeHtml(classUTName);
    setDynamicContent(`<p class="text-info">${t('loading_class_hints', safeClassName)}</p>`);

    try {
        const hints = await callGetHints(`type=CLASS&classUTName=${encodeURIComponent(classUTName)}`);

        const headerButtons = `
            <div class="d-flex justify-content-between align-items-center mb-3">
                <button onclick="renderClassHintsListView()" class="btn btn-sm btn-secondary">
                    <i class="fa fa-arrow-left"></i> ${t('back_class_list')}
                </button>
                ${(hints && hints.length > 0) ? `
                <button onclick="deleteAllHintsForSpecificClass('${safeClassName}')" class="btn btn-sm btn-danger">
                    <i class="fa fa-trash"></i> ${t('btn_delete_all_specific', safeClassName)}
                </button>` : ''}
            </div>
        `;

        if (!hints || hints.length === 0) {
            setDynamicContent(`
                <h2>${t('title_class_detail', safeClassName)}</h2>
                ${headerButtons}
                <p class="text-warning">${t('msg_no_hints')}</p>
            `);
            return;
        }

        hints.sort((a, b) => a.order - b.order);

        let tableHtml = `
            <h2>${t('title_class_detail', safeClassName)}</h2>
            ${headerButtons}
            <table class="table table-hover table-striped">
                <thead>
                    <tr>
                        <th style="width: 5%;">${t('col_actions')}</th>
                        <th style="width: 5%;">${t('col_order')}</th>
                        <th style="width: 20%;">${t('col_title')}</th>
                        <th style="width: 55%;">${t('col_content')}</th>
                        <th style="width: 15%;">${t('col_type')}</th>
                    </tr>
                </thead>
                <tbody>
        `;

        for (let i = 0; i < hints.length; i++) {
            const hint = hints[i];
            const safeName = escapeHtml(hint.name);
            const safeContentSnippet = escapeHtml(hint.content).substring(0, 100) + (hint.content.length > 100 ? '...' : '');
            const actionButtons = getActionButtons(hint.id, i === 0);

            tableHtml += `
                <tr onclick="renderHintDetailView('${hint.order}', 'CLASS', '${safeClassName}')" style="cursor:pointer;">
                    <td>${actionButtons}</td>
                    <td>${hint.order}</td>
                    <td><strong>${safeName}</strong></td>
                    <td>${safeContentSnippet}</td>
                    <td><span class="badge badge-info">${hint.type}</span></td>
                </tr>
            `;
        }

        tableHtml += `</tbody></table>`;
        setDynamicContent(tableHtml);

    } catch (error) {
        setDynamicContent(`<p class="text-danger">${t('msg_error_fetch', error.message)}</p>`);
    }
}

// --- VIEW 3: Dettaglio Suggerimento ---
async function renderHintDetailView(order, type, classUTName = null) {
    toggleInitialView(false);
    setDynamicContent(`<p class="text-info">${t('loading_detail')}</p>`);

    try {
        let urlParams = new URLSearchParams({ type: type, order: order }).toString();
        if (classUTName && classUTName !== 'null') {
            urlParams += `&classUTName=${encodeURIComponent(classUTName)}`;
        }

        const hints = await callGetHints(urlParams);
        const hintData = (Array.isArray(hints) && hints.length > 0) ? hints[0] : null;

        if (!hintData) {
            setDynamicContent(`<p class="text-warning">${t('msg_detail_not_found')}</p>`);
            return;
        }

        // SALVATAGGIO GLOBALE
        currentActiveHint = hintData;
        currentActiveHint.safeClassUTName = hintData.classUTName || 'null';

        const currentClassUTName = hintData.classUTName || 'null';
        const safeName = escapeHtml(hintData.name) || 'Senza Titolo';
        const safeContent = escapeHtml(hintData.content) || '-';
        const safeImageUri = escapeHtml(hintData.imageUri);
        const adminEmail = escapeHtml(hintData.adminEmail) || 'N/A';
        const createdAt = hintData.createdAt ? new Date(hintData.createdAt).toLocaleDateString() : 'N/D';

        let detailTitle = t('title_detail', safeName);

        let backFunction = (type === 'GENERIC' || currentClassUTName === 'null')
                           ? 'renderGenericHintsView()'
                           : `renderClassHintsDetailView('${escapeHtml(currentClassUTName)}')`;

        const detailHtml = `
            <h2>${detailTitle}</h2>
            <button onclick="${backFunction}" class="btn btn-secondary mb-3">
                <i class="fa fa-arrow-left"></i> ${t('back_list')}
            </button>

            <div class="card p-4">
                <p><strong>${t('label_title')}</strong> ${safeName}</p>
                <p><strong>${t('label_type')}</strong> <span class="badge badge-info">${hintData.type}</span></p>
                <p><strong>${t('label_order')}</strong> ${hintData.order}</p>
                <p><strong>${t('label_content')}</strong><br>${safeContent}</p>

                ${safeImageUri ?
                    `<div>
                        <p><strong>${t('label_image')}</strong></p>
                        <img src="${safeImageUri}" alt="Immagine Suggerimento" class="img-fluid border rounded" style="max-width: 600px; height: auto; margin-top: 5px;"/>
                    </div>`
                    : `<p><strong>${t('label_image')}</strong> <em>${t('label_no_image')}</em></p>`}

                <br>
                <p><strong>${t('label_creator')}</strong> ${adminEmail}</p>
                <p><strong>${t('label_date')}</strong> ${createdAt}</p>

                <div class="mt-3">
                    <button type="button" onclick="openEditModal()" class="btn btn-warning mr-2 text-white">
                        <i class="fa fa-pencil"></i> ${t('btn_edit')}
                    </button>

                    <button type="button"
                            onclick="deleteHintByClassUTAndOrder('${currentClassUTName}', ${hintData.order})"
                            class="btn btn-danger">
                        <i class="fa fa-trash"></i> ${t('btn_delete')}
                    </button>
                </div>
            </div>
        `;
        setDynamicContent(detailHtml);

    } catch (error) {
        setDynamicContent(`<p class="text-danger">${t('msg_error_fetch', error.message)}</p>`);
    }
}


// === GESTIONE MODIFICA E MODALI ===

function openEditModal() {
    if (!currentActiveHint) {
        alert(t('err_data_missing'));
        return;
    }
    document.getElementById('editHintForm').reset();

    document.getElementById('editHintId').value = currentActiveHint.id;
    document.getElementById('editHintName').value = currentActiveHint.name;
    document.getElementById('editHintContent').value = currentActiveHint.content;

    $('#editHintModal').modal('show');
}

async function submitEditHint() {
    const id = document.getElementById('editHintId').value;
    const name = document.getElementById('editHintName').value;
    const content = document.getElementById('editHintContent').value;
    const imageInput = document.getElementById('editHintImage');

    if (!name || !content) {
        alert(t('err_fields_required'));
        return;
    }

    const formData = new FormData();
    formData.append('name', name);
    formData.append('content', content);

    if (imageInput.files.length > 0) {
        formData.append('file', imageInput.files[0]);
    }

    const url = `/hints/update/${id}`;

    try {
        const token = getCookie("jwtToken");

        const response = await fetch(url, {
            method: 'PUT',
            headers: { 'Authorization': 'Bearer ' + token },
            body: formData
        });

        if (response.ok) {
            $('#editHintModal').modal('hide');

            if(currentActiveHint) {
                currentActiveHint.name = name;
                currentActiveHint.content = content;
            }

            document.getElementById('successModalLabel').innerText = t('title_success');
            document.getElementById('successMessageContent').innerText = t('msg_success_update');

            const successBtn = document.querySelector('#successModal .btn-primary');
            if(successBtn) successBtn.innerText = t('btn_continue');

            $('#successModal').modal('show');

        } else {
            const errorText = await response.text();
            alert(t('title_error') + ": " + errorText);
        }
    } catch (error) {
        console.error("Errore fetch update:", error);
        alert(t('err_communication'));
    }
}

function reloadCurrentHintDetail() {
    $('#successModal').modal('hide');

    if (currentActiveHint) {
        renderHintDetailView(
            currentActiveHint.order,
            currentActiveHint.type,
            currentActiveHint.safeClassUTName
        );
    } else {
        renderGenericHintsView();
    }
}

// === INIZIALIZZAZIONE ===
document.addEventListener("DOMContentLoaded", () => {
    // Traduzione testi statici iniziali
    const btnGeneric = document.getElementById('viewGenericHintsBtn');
    const btnClass = document.getElementById('viewClassHintsBtn');

    if(btnGeneric) {
        btnGeneric.innerHTML = `<i class="fa fa-list"></i> ${t('title_generic')}`;
        btnGeneric.addEventListener('click', renderGenericHintsView);
    }
    if(btnClass) {
        btnClass.innerHTML = `<i class="fa fa-list"></i> ${t('title_classes')}`;
        btnClass.addEventListener('click', renderClassHintsListView);
    }

    if(document.getElementById("searchForm")) {
        document.getElementById("searchForm").addEventListener("submit", handleSearchSubmit);
    }

    toggleInitialView(true);
});

// === ERROR DISPLAY ===
function displayError(message, containerId = null) {
    let errorContainer = containerId ? document.getElementById(containerId) : document.getElementById('api-error-alert-container');
    if (!errorContainer) errorContainer = document.getElementById('dynamic-content') || document.getElementById('main-view-container') || document.body;

    const oldAlert = document.getElementById('api-error-alert');
    if (oldAlert) oldAlert.remove();

    const errorHtml = `
        <div id="api-error-alert" class="alert alert-danger mt-3" role="alert" style="color: #842029; background-color: #f8d7da; border-color: #f5c2c7; padding: 15px; margin-bottom: 20px;">
            <strong>${t('title_error')}:</strong> ${escapeHtml(message)}
            <button type="button" class="close" data-dismiss="alert" aria-label="Close"
                    onclick="document.getElementById('api-error-alert').remove()">
                <span aria-hidden="true">&times;</span>
            </button>
        </div>
    `;

    errorContainer.insertAdjacentHTML('afterbegin', errorHtml);
    errorContainer.scrollIntoView({ behavior: 'smooth', block: 'start' });
}