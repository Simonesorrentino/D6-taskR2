package com.t4.gamerepo.service;

import com.t4.gamerepo.mapper.MapperFacade;
import com.t4.gamerepo.mapper.PlayerResultMapper;
import com.t4.gamerepo.model.*;
import com.t4.gamerepo.model.dto.request.CloseGameDTO;
import com.t4.gamerepo.model.dto.request.CloseTurnDTO;
import com.t4.gamerepo.model.dto.response.GameDTO;
import com.t4.gamerepo.model.dto.response.RoundDTO;
import com.t4.gamerepo.model.dto.response.TurnDTO;
import com.t4.gamerepo.model.repositories.GameRepository;
import com.t4.gamerepo.model.PlayerResult;
import com.t4.gamerepo.service.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import testrobotchallenge.commons.models.opponent.GameMode;
import testrobotchallenge.commons.models.opponent.OpponentDifficulty;
import testrobotchallenge.commons.models.opponent.OpponentType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service che gestisce le operazioni di CREATE, READ, UPDATE sulle partite.
 * <p>
 * Richiama {@link RoundService} per la gestione dei round.
 * </p>
 */
@Service
public class GameService {

    private final GameRepository gameRepository;
    private final RoundService roundService;

    private final Logger logger = LoggerFactory.getLogger(GameService.class);

    public GameService(GameRepository gameRepository, RoundService roundService) {
        this.gameRepository = gameRepository;
        this.roundService = roundService;
    }

    /**
     * Utility privata che recupera una partita dal database tramite il suo ID.
     *
     * @param gameId    l'ID del gioco
     * @return          il DTO dell'entità {@link Game} estratta
     * @throws GameNotFoundException    se la partita non esiste
     */
    private Game findGame(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));
    }

    /**
     * Recupera una partita dal database tramite il suo ID.
     *
     * @param gameId    l'ID del gioco
     * @return          il DTO dell'entità {@link Game} estratta
     * @throws GameNotFoundException    se la partita non esiste
     */
    public GameDTO getGameById(Long gameId) {
        Game game = findGame(gameId);
        return MapperFacade.toDTO(game);
    }

    /**
     * Recupera tutte le partite a cui ha partecipato un giocatore.
     *
     * @param playerId      l'ID del giocatore
     * @return              la lista di DTO delle partite estratte
     */
    public List<GameDTO> getAllPlayerGames(Long playerId) {
        List<Game> games = gameRepository.findByPlayerId(playerId);
        return games.stream().map(MapperFacade::toDTO).collect(Collectors.toList());
    }

    /**
     * Recupera tutte le partite presenti nel database.
     *
     * @return  la lista dei DTO di tutte le partite
     */
    public List<GameDTO> getAllGames() {
        List<Game> games = gameRepository.findAll();
        return games.stream().map(MapperFacade::toDTO).collect(Collectors.toList());
    }

    /**
     * Crea una nuova partita con i giocatori indicati per la modalità {@link GameMode} specificata.
     *
     * @param gameMode      la modalità di gioco
     * @param players       la lista di ID dei giocatori
     * @return              il DTO della partita inizializzata
     * @throws DuplicatedPlayersInGameException     se ci sono giocatori duplicati nella lista
     */
    @Transactional
    public GameDTO createGame(GameMode gameMode, List<Long> players) {
        Set<Long> playersSet = new HashSet<>(players);

        // Non è previsto che un giocatore possa giocare contro se stesso
        if (playersSet.size() != players.size())
            throw new DuplicatedPlayersInGameException("Duplicated players in game");

        Game newGame = new Game(gameMode, players);
        newGame.setStatus(GameStatus.CREATED);

        return MapperFacade.toDTO(gameRepository.save(newGame));
    }

    /**
     * Apre un nuovo round in una partita esistente. Il numero del round è automaticamente impostato al successivo del
     * precedente.
     *
     * @param gameId        l'ID della partita
     * @param classUT       la classe sotto test
     * @param type          il tipo di avversario
     * @param difficulty    la difficoltà dell'avversario
     * @return              il DTO del round avviato
     * @throws GameAlreadyClosedException       se la partita è già conclusa
     * @throws FoundRoundNotClosedException     se l'ultimo round registrato (precedente a questo) non è ancora stato chiuso
     */
    @Transactional
    public RoundDTO startRound(Long gameId, String classUT, OpponentType type, OpponentDifficulty difficulty) {
        Game game = findGame(gameId);

        if (game.getClosedAt() != null)
            throw new GameAlreadyClosedException("Game already closed");

        List<Round> rounds = game.getRounds();
        int roundNumber;
        if (rounds.isEmpty()) {
            roundNumber = 1;
        } else {
            if (game.getLastRound().getClosedAt() == null)
                throw new FoundRoundNotClosedException("The last round has not been closed, can't create a new round");

            roundNumber = game.getLastRound().getRoundNumber() + 1;
        }

        Round newRound = roundService.createRound(roundNumber, classUT, type, difficulty);
        game.addRound(newRound);
        game.setStatus(GameStatus.STARTED);
        gameRepository.save(game);

        return MapperFacade.toDTO(newRound);
    }

    /**
     * Avvia (crea) un turno per un giocatore in una partita esistente. Il turno sarà automaticamente associato
     * all'ultimo round aperto registrato per il game.
     *
     * @param gameId        l'ID della partita
     * @param playerId      l'ID del giocatore
     * @return              il DTO del turno creato
     * @throws GameAlreadyClosedException   se la partita è già conclusa
     * @throws PlayerNotInGameException     se il giocatore non è registrato nella partita
     * @throws RoundNotFoundException       se non esistono round nella partita
     */
    @Transactional
    public TurnDTO startTurn(Long gameId, Long playerId) {
        Game game = findGame(gameId);
        List<Round> rounds = game.getRounds();

        if (game.getClosedAt() != null)
            throw new GameAlreadyClosedException("Game already closed");

        if (!game.getPlayers().contains(playerId))
            throw new PlayerNotInGameException("Player not in game");

        if (rounds.isEmpty())
            throw new RoundNotFoundException("Round not found");

        Turn newTurn = roundService.startTurn(game.getLastRound(), playerId);

        game.setStatus(GameStatus.IN_PROGRESS);
        gameRepository.save(game);

        return MapperFacade.toDTO(newTurn);
    }

    /**
     * Termina un turno di un giocatore e registra il punteggio.
     *
     * @param gameId            l'ID della partita
     * @param turnNumber        il numero del turno nel round aperto
     * @param closeTurnDTO      il punteggio ottenuto dal giocatore
     * @return                  il turno concluso
     * @throws GameAlreadyClosedException   se la partita è già terminata
     * @throws PlayerNotInGameException     se il giocatore non è registrato nella partita
     * @throws RoundNotFoundException       se non esistono round nella partita
     */
    @Transactional
    public TurnDTO endTurn(Long gameId, int turnNumber, CloseTurnDTO closeTurnDTO) {
        Game game = findGame(gameId);
        List<Round> rounds = game.getRounds();
        Long playerId = closeTurnDTO.getPlayerId();
        TurnScore turnScore = MapperFacade.toEntity(closeTurnDTO);

        logger.info("Game rounds: {}", rounds);

        if (game.getClosedAt() != null) {
            logger.error("Game {} is already closed", gameId);
            throw new GameAlreadyClosedException("Game already closed");
        }

        if (!game.getPlayers().contains(playerId)) {
            logger.error("Player {} isn't register in game {}", playerId, gameId);
            throw new PlayerNotInGameException("Player not in game");
        }

        if (rounds.isEmpty()) {
            logger.error("Rounds not found for game {}", gameId);
            throw new RoundNotFoundException("Round not found");
        }

        Turn closedTurn = roundService.closeTurn(game.getLastRound(), turnNumber, playerId, turnScore);
        gameRepository.save(game);

        return MapperFacade.toDTO(closedTurn);
    }

    /**
     * Termina l'ultimo round aperto di una partita.
     *
     * @param gameId        l'ID della partita
     * @return              il round chiuso
     * @throws GameAlreadyClosedException   se la partita è gia conclusa
     * @throws RoundNotFoundException       se non esistono round nella partita
     */
    @Transactional
    public RoundDTO endRound(Long gameId) {
        Game game = findGame(gameId);
        List<Round> rounds = game.getRounds();

        if (game.getClosedAt() != null)
            throw new GameAlreadyClosedException("Game already closed");

        if (rounds.isEmpty())
            throw new RoundNotFoundException("Round not found");

        Round closedRound = roundService.closeRound(game.getLastRound());
        gameRepository.save(game);

        return MapperFacade.toDTO(closedRound);
    }

    /**
     * Termina una partita, registrando lo stato finale e i risultati dei giocatori.
     *
     * @param gameId            l'ID della partita
     * @param closeGameDTO      il DTO ricevuto come corpo della richiesta
     * @return                  la partita conclusa
     * @throws GameAlreadyClosedException   se la partita è già terminata
     */
    @Transactional
    public GameDTO endGame(Long gameId, CloseGameDTO closeGameDTO) {
        Game game = findGame(gameId);
        Map<Long, PlayerResult> playersResult = new HashMap<>();
        for (Long playerId : closeGameDTO.getResults().keySet())
            playersResult.put(playerId, MapperFacade.toEntity(closeGameDTO.getResults().get(playerId)));

        if (game.getClosedAt() != null)
            throw new GameAlreadyClosedException("Game already closed");

        game.setStatus(GameStatus.FINISHED);
        game.setClosedAt(Timestamp.from(Instant.now()));
        game.setPlayerResults(playersResult);

        return MapperFacade.toDTO(gameRepository.save(game));
    }
}
