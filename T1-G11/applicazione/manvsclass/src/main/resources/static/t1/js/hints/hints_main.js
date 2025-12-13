/*
 * Copyright (c) 2025 Stefano Marano https://github.com/StefanoMarano80017
 * All rights reserved.
 * Versione Definitiva: UI con Pulsanti "Elimina Tutti".
 */

// Mappa id pulsanti a view
const links = {
    navDashboardAdmin: VIEWS.DASHBOARD_ADMIN,
    navHintsMain: VIEWS.HINTS_MAIN,
    navDashboardAdmin2: VIEWS.DASHBOARD_ADMIN,
    navHintsUpload: VIEWS.HINTS_UPLOAD,
};
assignUrls(links);


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
    if (initialButtons) {
        initialButtons.style.display = show ? 'flex' : 'none';
    }

    const navbarContent = document.getElementById('navbarSupportedContent');
    if (navbarContent) {
        navbarContent.style.display = show ? 'block' : 'none';
    }
}

function setDynamicContent(html) {
    document.getElementById('dynamic-content').innerHTML = html;
}

// === FUNZIONI DI ELIMINAZIONE ===

// 1. Elimina singolo suggerimento (esistente)
async function deleteHintByClassUTAndOrder(classUTName, order) {
    if (!confirm(messages.confirmDeleteSingle)) {
        return;
    }

    const safeClassUTName = classUTName === 'null' ? 'null' : encodeURIComponent(classUTName);
    const url = `${APIS.DELETE_HINT_BY_CLASS_AND_ORDER}/${safeClassUTName}/order/${order}`;

    await callDeleteHint(url);
}

// 2. Elimina TUTTI i suggerimenti di una specifica classe (NUOVO)
async function deleteAllHintsForSpecificClass(classUTName) {
    if (!confirm(`Sei sicuro di voler eliminare TUTTI i suggerimenti per la classe ${classUTName}?`)) {
        return;
    }
    // Assume che l'endpoint sia .../hints/class/{NomeClasse}
    const safeClassUTName = encodeURIComponent(classUTName);
    const url = `${APIS.DELETE_HINT_BY_CLASS}/${safeClassUTName}`;

    await callDeleteHint(url);
}

// 3. Elimina TUTTI i suggerimenti di tipo CLASS (Globale) (ABILITATO)
async function deleteAllClassHints() {
    if (!confirm("Sei sicuro di voler eliminare TUTTI i suggerimenti di tipo CLASS dal sistema? Questa operazione è irreversibile.")) {
        return;
    }
    // NOTA: Assicurati che il Backend supporti questa chiamata.
    // Se l'endpoint per cancellare tutti i CLASS è diverso, modifica l'URL qui sotto.
    // Esempio ipotetico: /hints/type/CLASS
    const url = `${APIS.DELETE_HINT_BY_TYPE}/CLASS`;
    // Se non hai un endpoint by Type, ma usi quello by Class 'all', adatta di conseguenza.

    await callDeleteHint(url);
}

// 4. Elimina TUTTI i suggerimenti Generici (ESISTENTE)
async function deleteAllGenericHints() {
    if (!confirm(messages.confirmDeleteGeneric)) {
        return;
    }
    // Endpoint: .../hints/class/null (come da tua configurazione precedente)
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
        {
            reload: true
        });
    } catch (error) {
        let errorMessage = "Errore sconosciuto nel Back-End.";

        if (error.response && error.response.message) {
            errorMessage = error.response.message;
        } else if (error.message) {
             errorMessage = error.message;
        } else if (typeof error === 'string') {
             errorMessage = error;
        }

        displayError(errorMessage);
        return;
    }
}

// === GESTIONE DELLA RICERCA ===

function handleSearchSubmit(event) {
    event.preventDefault();
    const searchValue = document.getElementById('searchInput').value.trim();
    if (searchValue) {
        alert("La funzionalità di ricerca non è ancora pienamente supportata.");
    }
    document.location.reload();
}

// === VISTE DI RENDERING ===

// --- VIEW 1: Elenco Suggerimenti Generici ---
async function renderGenericHintsView() {
    toggleInitialView(false);
    setDynamicContent('<p class="text-info">Caricamento suggerimenti generici...</p>');

    try {
        const hints = await callGetHints("type=GENERIC");

        // Bottone Back + Bottone Elimina Tutto
        const headerButtons = `
            <div class="d-flex justify-content-between align-items-center mb-3">
                <button onclick="document.location.reload()" class="btn btn-sm btn-secondary">
                    <i class="fa fa-arrow-left"></i> Torna alla selezione
                </button>
                ${(hints && hints.length > 0) ? `
                <button onclick="deleteAllGenericHints()" class="btn btn-sm btn-danger">
                    <i class="fa fa-trash"></i> Elimina Tutti i Generici
                </button>` : ''}
            </div>
        `;

        if (!hints || hints.length === 0) {
            setDynamicContent(`
                <h2>Suggerimenti Generici</h2>
                ${headerButtons}
                <p class="text-warning">Nessun suggerimento generico trovato.</p>
            `);
            return;
        }

        let tableHtml = `
            <h2>Suggerimenti Generici</h2>
            ${headerButtons}
            <table class="table table-hover table-striped">
                <thead>
                    <tr>
                        <th style="width: 10%;">Ordine</th>
                        <th style="width: 70%;">Contenuto</th>
                        <th style="width: 20%;">Tipo</th>
                    </tr>
                </thead>
                <tbody>
        `;

        for (const hint of hints) {
            const safeContentSnippet = escapeHtml(hint.content).substring(0, 150) + (hint.content.length > 150 ? '...' : '');
            const safeOrder = hint.order;

            tableHtml += `
                <tr onclick="renderHintDetailView('${safeOrder}', 'GENERIC', 'null')" style="cursor:pointer;">
                    <td>#${safeOrder}</td>
                    <td>${safeContentSnippet}</td>
                    <td>Generico</td>
                </tr>
            `;
        }

        tableHtml += `</tbody></table>`;
        setDynamicContent(tableHtml);

    } catch (error) {
        console.error("Errore generico:", error);
        setDynamicContent(`<p class="text-danger">Errore nel caricamento: ${error.message}.</p>`);
    }
}

// --- VIEW 2: Elenco Classi ---
async function renderClassHintsListView() {
    toggleInitialView(false);
    setDynamicContent('<p class="text-info">Caricamento classi...</p>');

    try {
        const hints = await callGetHints("type=CLASS");

        // Bottone Back + Bottone Elimina Tutto (Globale Classi)
        const headerButtons = `
            <div class="d-flex justify-content-between align-items-center mb-3">
                <button onclick="document.location.reload()" class="btn btn-sm btn-secondary">
                    <i class="fa fa-arrow-left"></i> Torna alla selezione
                </button>
                ${(hints && hints.length > 0) ? `
                <button onclick="deleteAllClassHints()" class="btn btn-sm btn-danger">
                    <i class="fa fa-trash"></i> Elimina TUTTI i Class Hints
                </button>` : ''}
            </div>
        `;

        if (!hints || hints.length === 0) {
            setDynamicContent(`
                <h2>Classi con Suggerimenti</h2>
                ${headerButtons}
                <p class="text-warning">Nessun suggerimento di classe trovato.</p>
            `);
            return;
        }

        const classGroup = hints.reduce((acc, hint) => {
            const className = hint.classUTName;
            if (className) {
                acc[className] = (acc[className] || 0) + 1;
            }
            return acc;
        }, {});

        const sortedClassNames = Object.keys(classGroup).sort();

        let tableHtml = `
            <h2>Classi con Suggerimenti</h2>
            ${headerButtons}
            <table class="table table-hover table-striped">
                <thead>
                    <tr>
                        <th style="width: 70%;">Nome Classe</th>
                        <th style="width: 30%;">Numero Suggerimenti</th>
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
        console.error("Errore classi:", error);
        setDynamicContent(`<p class="text-danger">Errore nel caricamento: ${error.message}.</p>`);
    }
}

// --- VIEW 2.1: Elenco Suggerimenti Classe Specifica ---
async function renderClassHintsDetailView(classUTName) {
    toggleInitialView(false);
    const safeClassName = escapeHtml(classUTName);
    setDynamicContent(`<p class="text-info">Caricamento suggerimenti per <strong>${safeClassName}</strong>...</p>`);

    try {
        const hints = await callGetHints(`type=CLASS&classUTName=${encodeURIComponent(classUTName)}`);

        // Bottone Back + Bottone Elimina Tutto per QUESTA classe
        const headerButtons = `
            <div class="d-flex justify-content-between align-items-center mb-3">
                <button onclick="renderClassHintsListView()" class="btn btn-sm btn-secondary">
                    <i class="fa fa-arrow-left"></i> Torna all'elenco Classi
                </button>
                ${(hints && hints.length > 0) ? `
                <button onclick="deleteAllHintsForSpecificClass('${safeClassName}')" class="btn btn-sm btn-danger">
                    <i class="fa fa-trash"></i> Elimina Tutti per ${safeClassName}
                </button>` : ''}
            </div>
        `;

        if (!hints || hints.length === 0) {
            setDynamicContent(`
                <h2>Suggerimenti per ${safeClassName}</h2>
                ${headerButtons}
                <p class="text-warning">Nessun suggerimento trovato.</p>
            `);
            return;
        }

        let tableHtml = `
            <h2>Suggerimenti per ${safeClassName}</h2>
            ${headerButtons}
            <table class="table table-hover table-striped">
                <thead>
                    <tr>
                        <th style="width: 10%;">Ordine</th>
                        <th style="width: 70%;">Contenuto</th>
                        <th style="width: 20%;">Tipo</th>
                    </tr>
                </thead>
                <tbody>
        `;

        for (const hint of hints) {
            const safeContentSnippet = escapeHtml(hint.content).substring(0, 150) + (hint.content.length > 150 ? '...' : '');
            const safeOrder = hint.order;

            tableHtml += `
                <tr onclick="renderHintDetailView('${safeOrder}', 'CLASS', '${safeClassName}')" style="cursor:pointer;">
                    <td>#${safeOrder}</td>
                    <td>${safeContentSnippet}</td>
                    <td>${hint.type}</td>
                </tr>
            `;
        }

        tableHtml += `</tbody></table>`;
        setDynamicContent(tableHtml);

    } catch (error) {
        console.error(`Errore suggerimenti classe ${classUTName}:`, error);
        setDynamicContent(`<p class="text-danger">Errore nel caricamento: ${error.message}.</p>`);
    }
}

// --- VIEW 3: Dettaglio Suggerimento (INVARIATO) ---
async function renderHintDetailView(order, type, classUTName = null) {
    toggleInitialView(false);
    setDynamicContent('<p class="text-info">Caricamento dettaglio suggerimento...</p>');

    try {
        let urlParams = new URLSearchParams({ type: type, order: order }).toString();

        if (classUTName && classUTName !== 'null') {
            urlParams += `&classUTName=${encodeURIComponent(classUTName)}`;
        }

        const hints = await callGetHints(urlParams);
        const hintData = (Array.isArray(hints) && hints.length > 0) ? hints[0] : null;

        if (!hintData) {
            setDynamicContent('<p class="text-warning">Dettaglio suggerimento non trovato.</p>');
            return;
        }

        const currentClassUTName = hintData.classUTName || 'null';
        const safeClassUTName = escapeHtml(currentClassUTName) || 'Generico';
        const adminEmail = escapeHtml(hintData.adminEmail) || 'N/A';
        const createdAt = hintData.createdAt ? new Date(hintData.createdAt).toLocaleDateString() : 'N/D';
        const safeContent = escapeHtml(hintData.content) || 'Nessun contenuto definito';
        const safeImageUri = escapeHtml(hintData.imageUri);

        let detailTitle = "";
        if (type === 'GENERIC' || currentClassUTName === 'null') {
            detailTitle = `Dettaglio Suggerimento Generico #${hintData.order}`;
        } else {
            detailTitle = `Dettaglio Suggerimento #${hintData.order} (${safeClassUTName})`;
        }

        let backFunction = (type === 'GENERIC' || currentClassUTName === 'null')
                           ? 'renderGenericHintsView()'
                           : `renderClassHintsDetailView('${escapeHtml(currentClassUTName)}')`;

        const detailHtml = `
            <h2>${detailTitle}</h2>
            <button onclick="${backFunction}" class="btn btn-secondary mb-3">
                <i class="fa fa-arrow-left"></i> Torna all'elenco
            </button>

            <p><strong>Tipo:</strong> ${hintData.type}</p>
            <p><strong>Ordine:</strong> ${hintData.order}</p>
            <p><strong>Contenuto:</strong> ${safeContent}</p>

            ${safeImageUri ?
                `<div>
                    <p><strong>Immagine:</strong></p>
                    <img src="${safeImageUri}" alt="Immagine Suggerimento" class="img-fluid border rounded" style="max-width: 600px; height: auto; margin-top: 5px;"/>
                </div>`
                : '<p><strong>Immagine:</strong> <em>Nessuna immagine allegata.</em></p>'}

            <br>
            <p><strong>Creatore:</strong> ${adminEmail}</p>
            <p><strong>Data Creazione:</strong> ${createdAt}</p>

            <button type="button"
                    onclick="deleteHintByClassUTAndOrder('${currentClassUTName}', ${hintData.order})"
                    class="btn btn-icon btn-danger mt-3">
                <i class="fa fa-trash"></i> Elimina Suggerimento
            </button>
        `;
        setDynamicContent(detailHtml);

    } catch (error) {
        console.error("Errore dettaglio:", error);
        setDynamicContent(`<p class="text-danger">Errore nel caricamento: ${error.message}.</p>`);
    }
}

// === INIZIALIZZAZIONE ===
document.addEventListener("DOMContentLoaded", () => {
    document.getElementById('viewGenericHintsBtn').addEventListener('click', renderGenericHintsView);
    document.getElementById('viewClassHintsBtn').addEventListener('click', renderClassHintsListView);
    document.getElementById("searchForm").addEventListener("submit", handleSearchSubmit);
    toggleInitialView(true);
});

// === ERROR DISPLAY ===
function displayError(message, containerId = null) {
    let errorContainer;
    if (containerId) {
        errorContainer = document.getElementById(containerId);
    }
    if (!errorContainer) {
        errorContainer = document.getElementById('api-error-alert-container');
    }
    if (!errorContainer) {
        errorContainer = document.getElementById('dynamic-content') ||
                         document.getElementById('main-view-container');
    }
    if (!errorContainer) {
        errorContainer = document.getElementById('main-content') || document.body;
    }

    if (!errorContainer) return;

    const oldAlert = document.getElementById('api-error-alert');
    if (oldAlert) oldAlert.remove();

    const errorHtml = `
        <div id="api-error-alert" class="alert alert-danger mt-3" role="alert" style="color: #842029; background-color: #f8d7da; border-color: #f5c2c7; padding: 15px; margin-bottom: 20px;">
            <strong>Errore:</strong> ${escapeHtml(message)}
            <button type="button" class="close" data-dismiss="alert" aria-label="Close"
                    onclick="document.getElementById('api-error-alert').remove()">
                <span aria-hidden="true">&times;</span>
            </button>
        </div>
    `;

    errorContainer.insertAdjacentHTML('afterbegin', errorHtml);
    errorContainer.scrollIntoView({ behavior: 'smooth', block: 'start' });
}