/*
 * Copyright (c) 2025 Stefano Marano
 * Gestione Upload Suggerimenti
 */

document.addEventListener("DOMContentLoaded", () => {
    const uploadForm = document.getElementById('uploadHintsForm');

    if (uploadForm) {
        uploadForm.addEventListener('submit', handleUploadSubmit);
    }
});

async function handleUploadSubmit(event) {
    // 1. BLOCCA IL RELOAD DELLA PAGINA
    event.preventDefault();

    // 2. Recupera i file
    const fileInput = document.getElementById('hintsFile');
    const imagesInput = document.getElementById('hintImages');
    const submitBtn = event.target.querySelector('button[type="submit"]');

    if (!fileInput.files.length) {
        alert("Seleziona il file JSON!");
        return;
    }

    // 3. Prepara il FormData
    const formData = new FormData();
    formData.append('file', fileInput.files[0]);

    if (imagesInput.files.length > 0) {
        for (let i = 0; i < imagesInput.files.length; i++) {
            formData.append('images', imagesInput.files[i]);
        }
    }

    // 4. Feedback visivo (Disabilita bottone e mostra caricamento)
    const originalBtnText = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = `<i class="fa fa-spinner fa-spin"></i> Caricamento...`;

    try {
        const token = getCookie("jwtToken"); // Assicurati che cookie_util.js sia incluso

        // 5. Chiamata al Backend
        const response = await fetch('/hints/upload', {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + token
                // NOTA: Non impostare 'Content-Type': 'multipart/form-data' manualmente!
                // Il browser lo fa in automatico con il boundary corretto quando usi FormData.
            },
            body: formData
        });

        if (response.ok) {
            // Successo
            alert("Suggerimenti caricati con successo!");
            // Pulisce il form
            document.getElementById('uploadHintsForm').reset();
        } else {
            // Errore dal server
            const errorText = await response.text();
            alert("Errore durante il caricamento: " + errorText);
        }

    } catch (error) {
        console.error("Errore di rete:", error);
        alert("Errore di comunicazione con il server.");
    } finally {
        // 6. Ripristina bottone
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalBtnText;
    }
}