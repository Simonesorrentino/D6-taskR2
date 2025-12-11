/*      Utility functions        */

function executeFetch(url, init) {
    return fetch(url, {
        ...init,
        credentials: 'include'
    });
}

async function handleApiErrors(response) {
    try {
        const errorBody = await response.json();
        console.error('Errore dalla risposta:', errorBody);

        if (errorBody?.errors?.length > 0) {
            errorBody.errors.forEach(err => {
                const container = document.getElementById(`${err.field}_label_container`);
                if (container) {
                    addErrorDiv(container, err.message);
                } else {
                    alert(err.message);
                }
            });
        } else {
            alert(errors.notHandled);
        }
    } catch (e) {
        console.error('Errore durante la lettura del corpo JSON:', e);
        alert(errors.notHandled);
    }
}

/**
 * Effettua una chiamata fetch e ritorna i dati se la risposta è ok.
 * In caso di errore chiama handleApiErrors.
 *
 * @param {Object} param0 parametri di richiesta: url, method, headers, body
 * @param {Function} parseResponse funzione async per parsare response (es: r => r.json())
 * @returns dati parsati o null in caso di errore
 */
async function returnDataOnSuccessTemplate({ url, method, headers, body }, parseResponse) {
    try {
        const init = { method };

        // Inserisce headers solo se definiti
        if (headers) init.headers = headers;

        // Inserisce body solo se definito e diverso da null
        if (body !== undefined && body !== null) init.body = body;

        const response = await executeFetch(url, init);

        if (response.ok) {
            if (parseResponse) {
                return await parseResponse(response);
            } else {
                return null;
            }
        }

        await handleApiErrors(response);
        return null;
    } catch (err) {
        console.error(`Errore nella chiamata ${method} ${url}:`, err);
        alert(errors.notHandled);
        return null;
    }
}

/**
 * Effettua una chiamata fetch e se ok reindirizza o ricarica la pagina.
 * In caso di errore chiama handleApiErrors.
 *
 * @param {Object} param0 parametri di richiesta: url, method, headers, body
 * @param {Object} param1 opzioni: redirectTo (stringa URL opzionale), reload (booleano, default false)
 */
async function redirectOnSuccessTemplate({ url, method, headers, body }, { redirectTo, reload = false }) {
    try {
        const init = { method };

        if (headers) init.headers = headers;
        if (body !== undefined && body !== null) init.body = body;

        const response = await executeFetch(url, init);

        if (response.ok) {
            if (reload) {
                location.reload();
            } else if (redirectTo) {
                window.location.href = redirectTo;
            }
            return;
        }

        await handleApiErrors(response);
    } catch (err) {
        console.error(`Errore nella chiamata ${method} ${url}:`, err);
        alert(errors.notHandled);
    }
}


/*      API calls        */


async function callLogoutAdmin() {
    await redirectOnSuccessTemplate(
        {
            url: APIS.USER_SERVICE.LOGOUT_ADMIN,
            method: "POST",
            headers: { 'Content-Type': 'application/json' },
            body: null
        },
        {
            redirectTo: VIEWS.LOGIN_USER,
            reload: false
        }
    );
}

async function callChangeLanguage(lang) {

}

async function callGetAllAdmins() {
    return await returnDataOnSuccessTemplate({
        url: APIS.USER_SERVICE.ALL_ADMINS,
        method: "GET",
        headers: { 'Content-Type': 'application/json' }
    }, async response => await response.json());
}

async function callGetAllPlayers() {
    return await returnDataOnSuccessTemplate({
        url: APIS.USER_SERVICE.ALL_PLAYERS,
        method: "GET",
        headers: { 'Content-Type': 'application/json' }
    }, async response => await response.json());
}

async function callGetAllGames() {
    return await returnDataOnSuccessTemplate({
        url: APIS.GAMEREPO_SERVICE.ALL_GAMES,
        method: "GET",
        headers: { 'Content-Type': 'application/json' }
    }, async response => await response.json());
}

async function callDownloadClassUT(className) {
    return await returnDataOnSuccessTemplate({
        url: `${APIS.DOWNLOAD_CLASSUT}/${className}`,
        method: "GET",
        headers: { 'Content-Type': 'application/json' }
    }, async response => await response.blob());
}

async function callDeleteClassUT(className) {
    await redirectOnSuccessTemplate({
        url: `${APIS.DELETE_OPPONENT}/${className}`,
        method: "DELETE",
        headers: { 'Content-Type': 'application/json' },
    },
    {
        reload: true
    });
}

async function callUploadOpponent(body) {
    return await returnDataOnSuccessTemplate({
        url: APIS.UPLOAD_OPPONENT,
        method: "POST",
        body: body
    }, async response => await response.json());
}

//Gestione Suggerimenti
/**
 * Recupera la lista dei suggerimenti, opzionalmente filtrati tramite queryParams.
 * @param {string} jwtToken - Il token JWT.
 * @param {Object} queryParams - Mappa di parametri per la query string (es. { type: 'class', classUTName: 'MyClass' }).
 * @returns {Array<Object> | null} Lista dei suggerimenti o null in caso di errore.
 */
async function callGetHints(jwtToken, queryParams = {}) {
    // Costruisce la query string
    const queryString = new URLSearchParams(queryParams).toString();
    const url = `${APIS.HINTS_SERVICE.BASE}?${queryString}`;

    // Nota: Il token JWT viene gestito dalle credenziali e non deve essere passato esplicitamente nell'header in questa configurazione.
    return await returnDataOnSuccessTemplate({
        url: url,
        method: "GET",
        headers: { 'Content-Type': 'application/json' }
    }, async response => await response.json());
}

/**
 * Carica nuovi suggerimenti e i relativi file immagine.
 * Usa FormData, quindi non specifica Content-Type.
 * @param {FormData} body - Il FormData contenente il file JSON e i file immagine.
 * @param {string} jwtToken - Il token JWT.
 * @returns {Object | null} Messaggio di successo o null in caso di errore.
 */
async function callUploadHints(body, jwtToken) {
    // Si noti che l'header Content-Type NON è specificato qui,
    // poiché il browser lo imposta automaticamente (multipart/form-data) quando si usa FormData.
    return await returnDataOnSuccessTemplate({
        url: APIS.HINTS_SERVICE.UPLOAD,
        method: "POST",
        // Body è FormData
        body: body
    }, async response => await response.json());
}


/**
 * Elimina tutti i suggerimenti per una specifica classe UT o tutti i suggerimenti generici.
 * @param {string} classUT - Il nome della classe UT o 'null' per i generici.
 * @param {string} jwtToken - Il token JWT.
 */
async function callDeleteHintByClass(classUT, jwtToken) {
    // Utilizziamo redirectOnSuccessTemplate per ricaricare la pagina in caso di successo
    // Se non vuoi ricaricare la pagina, dovrai usare returnDataOnSuccessTemplate
    await redirectOnSuccessTemplate({
        url: `${APIS.HINTS_SERVICE.BASE}/${classUT}`,
        method: "DELETE",
        headers: { 'Content-Type': 'application/json' },
    },
    {
        reload: true // Ricarica la pagina dopo l'eliminazione massiva
    });
}

/**
 * Elimina un singolo suggerimento per classe/tipo e ordine.
 * @param {string} classUT - Il nome della classe UT o 'null' per i generici.
 * @param {number} order - L'ordine del suggerimento.
 * @param {string} jwtToken - Il token JWT.
 * @returns {Object | null} Messaggio di successo o null in caso di errore.
 */
async function callDeleteHintByOrder(classUT, order, jwtToken) {
    // Utilizziamo returnDataOnSuccessTemplate per poter gestire il riordino in hints_management.js
    return await returnDataOnSuccessTemplate({
        url: `${APIS.HINTS_SERVICE.BASE}/${classUT}/${order}`,
        method: "DELETE",
        headers: { 'Content-Type': 'application/json' },
    }, async response => await response.json());
}





