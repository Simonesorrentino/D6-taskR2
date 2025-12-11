/*
 * Logica Frontend per la pagina di Gestione Suggerimenti (hints_management.html)
 */

// URL di fallback per la lingua (come nei tuoi altri script)
const dataTable_lang_urls = {
    it: "/t1/js/i18n/it-IT.json",
    en: "/t1/js/i18n/en-GB.json",
};
const lang = getCookie("lang");

// Funzione principale per inizializzare la pagina
document.addEventListener("DOMContentLoaded", async function () {
    // Assumiamo che la getCookie e VIEWS.LOGIN siano definiti
    const jwtToken = getCookie("jwt");

    if (!jwtToken) {
        console.error("JWT non trovato. Accesso non autorizzato.");
        // window.location.href = VIEWS.LOGIN;
        return;
    }

    // 1. Inizializza la tabella dei suggerimenti
    await loadHintsTable(jwtToken);

    // 2. Aggiungi i listener per l'Upload
    initUploadForm(jwtToken);

    // 3. Aggiungi il listener per l'Eliminazione Massiva
    initMassDeleteForm(jwtToken);
});

/**
 * Carica e inizializza la DataTables con i suggerimenti esistenti.
 * @param {string} jwtToken - Il token JWT per l'autenticazione.
 */
async function loadHintsTable(jwtToken) {
    const hints = await callGetHints(jwtToken, {});

    const tableData = hints.map(hint => {
        return {
            ...hint,
            actions: `
                <button
                    class="btn-delete-single"
                    data-type="${hint.type}"
                    data-class="${hint.classUTName || 'null'}"
                    data-order="${hint.order}">
                    <i class="fa fa-trash"></i> Elimina
                </button>
                <button
                    class="btn-edit-single disabled"
                    data-id="${hint.id}">
                    <i class="fa fa-edit"></i> Modifica
                </button>
            `
        };
    });

    if (tableData) {
        const table = $('#hintsTable').DataTable({
            destroy: true,
            data: tableData,
            columns: [
                { data: 'id' },
                { data: 'type' },
                { data: 'classUTName', defaultContent: 'Generico' },
                { data: 'content' },
                { data: 'order' },
                { data: 'imageUri', defaultContent: 'No', render: (data, type, row) => {
                    return data ? '<i class="fa fa-image" style="color: green;"></i> Sì' : 'No';
                }},
                { data: 'actions', orderable: false }
            ],
            language: {
                url: dataTable_lang_urls[lang],
            },
            responsive: true
        });

        $('#hintsTable tbody').on('click', '.btn-delete-single', handleDeleteSingleHint);

    } else {
        console.warn("Nessun suggerimento da caricare o errore API.");
    }
}

/**
 * Inizializza il form di upload per la creazione massiva dei suggerimenti.
 * @param {string} jwtToken - Il token JWT.
 */
function initUploadForm(jwtToken) {
    const form = document.getElementById("uploadHintsForm");

    form.addEventListener("submit", async function (event) {
        event.preventDefault();

        const submitButton = form.querySelector('button[type="submit"]');
        submitButton.disabled = true;
        submitButton.textContent = "Caricamento in corso...";

        const formData = new FormData(form);

        try {
            const result = await callUploadHints(formData, jwtToken);

            alert("Successo: " + (result.message || "Suggerimenti caricati con successo."));

            await loadHintsTable(jwtToken);
            form.reset();

        } catch (error) {
            const errorMessage = error.message || "Errore sconosciuto durante l'upload.";
            // Assumiamo che renderErrorAlert sia disponibile
            // renderErrorAlert(errorMessage);
            console.error("Errore Upload Suggerimenti:", error);

        } finally {
            submitButton.disabled = false;
            submitButton.textContent = "Carica Suggerimenti";
        }
    });
}

/**
 * Gestisce l'eliminazione di un singolo suggerimento.
 */
async function handleDeleteSingleHint() {
    const jwtToken = getCookie("jwt");
    const classUt = this.getAttribute('data-class');
    const order = this.getAttribute('data-order');
    const type = this.getAttribute('data-type');

    const target = (type === 'GENERIC') ? `il suggerimento generico con ordine ${order}` : `il suggerimento per la classe ${classUt} con ordine ${order}`;

    if (!confirm(`Sei sicuro di voler eliminare ${target}? L'operazione è irreversibile e riordinerà i suggerimenti successivi.`)) {
        return;
    }

    try {
        const result = await callDeleteHintByOrder(classUt, order, jwtToken);

        alert(result.message || "Suggerimento eliminato e lista riordinata con successo.");

        await loadHintsTable(jwtToken);

    } catch (error) {
        const errorMessage = error.message || "Errore durante l'eliminazione del suggerimento.";
        // renderErrorAlert(errorMessage);
        console.error("Errore Eliminazione Singola:", error);
    }
}


/**
 * Inizializza il form per l'eliminazione massiva dei suggerimenti (per classe o generici).
 * LOGICA CORRETTA PER GESTIRE INPUT DI TESTO E SELEZIONE 'GENERICO'.
 * @param {string} jwtToken - Il token JWT.
 */
function initMassDeleteForm(jwtToken) {
    const form = document.getElementById("massDeleteHintsForm");

    form.addEventListener("submit", async function (event) {
        event.preventDefault();

        const selectElement = document.getElementById("classUtMassDelete");
        const classUtNameInput = document.getElementById("classUtNameInput");

        let classUtToDelete;
        let target;

        // 1. Determina l'azione e il target
        if (selectElement.value === 'null') {
            // Caso 1: Eliminazione Generici
            classUtToDelete = 'null';
            target = 'tutti i suggerimenti GENERICI';
        }
        else if (classUtNameInput.value.trim() !== '') {
            // Caso 2: Eliminazione per Classe
            classUtToDelete = classUtNameInput.value.trim();
            target = `tutti i suggerimenti per la classe ${classUtToDelete}`;
        }
        else {
            alert("Per l'eliminazione per classe, inserisci un nome di Classe UT valido.");
            return;
        }


        if (!confirm(`ATTENZIONE! Sei sicuro di voler eliminare ${target}? L'operazione è irreversibile e include l'eliminazione dei file immagine associati.`)) {
            return;
        }

        try {
            const result = await callDeleteHintByClass(classUtToDelete, jwtToken);

            alert(result.message || "Suggerimenti eliminati con successo.");

            await loadHintsTable(jwtToken);
            form.reset();

        } catch (error) {
            const errorMessage = error.message || "Errore durante l'eliminazione massiva.";
            // renderErrorAlert(errorMessage);
            console.error("Errore Eliminazione Massiva:", error);
        }
    });
}