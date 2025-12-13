/*
 * hints_upload.js
 * Gestione upload diretta per bypassare gli alert globali.
 */

async function uploadHints() {
    const hintFileInput = document.getElementById('hintFile');
    const imageFilesInput = document.getElementById('imageFiles');
    const loadingOverlay = document.getElementById('loadingOverlay');

    const hintFile = hintFileInput.files[0];
    const imageFiles = imageFilesInput.files;

    // 1. Validazione input
    if (!hintFile) {
        showCustomError("Seleziona il file JSON dei suggerimenti.");
        return;
    }

    // 2. Preparazione dati
    const formData = new FormData();
    formData.append('file', hintFile);

    if (imageFiles && imageFiles.length > 0) {
        for (let i = 0; i < imageFiles.length; i++) {
            formData.append('images', imageFiles[i]);
        }
    }

    if(loadingOverlay) loadingOverlay.style.display = 'block';

    // 3. CHIAMATA DIRETTA AJAX (Bypassa callUploadHints e i suoi alert)
    // Assicurati che l'URL '/hints/upload' sia quello corretto del tuo Controller Spring
    $.ajax({
        url: '/hints/upload',
        type: 'POST',
        data: formData,
        processData: false, // Indispensabile per FormData
        contentType: false, // Indispensabile per FormData
        success: function(response) {
            if(loadingOverlay) loadingOverlay.style.display = 'none';

            // Successo: Mostra la modale di successo esistente
            $('#successModal').modal('show');
        },
        error: function(xhr, status, error) {
            if(loadingOverlay) loadingOverlay.style.display = 'none';
            console.error("Errore upload:", xhr);

            let errorMessage = "Si è verificato un errore durante il caricamento.";

            // --- Estrazione Messaggio Errore (Spring Boot) ---
            // Analizza la risposta JSON (quella che vedi nel tab Network)
            if (xhr.responseJSON && xhr.responseJSON.message) {
                // Caso ideale: Spring ha ritornato un JSON pulito
                errorMessage = xhr.responseJSON.message;
            }
            else if (xhr.responseText) {
                // Caso fallback: prova a parsare la stringa se non è stata convertita autom.
                try {
                    const parsed = JSON.parse(xhr.responseText);
                    if (parsed.message) {
                        errorMessage = parsed.message;
                    }
                } catch(e) {
                    // Se non è JSON, potrebbe essere un errore generico
                    console.warn("Impossibile parsare responseText");
                }
            }

            // 4. Mostra la NOSTRA modale di errore
            showCustomError(errorMessage);
        }
    });
}

/**
 * Funzione per aprire la modale di errore custom
 */
function showCustomError(message) {
    const msgEl = document.getElementById('customErrorText');
    if(msgEl) msgEl.textContent = message;

    $('#customErrorModal').modal('show');
}