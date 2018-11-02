import java.util.ArrayList;

public class Round {

	ArrayList<Player> players;
	ArrayList<Player> lastChancePlayers;
	Turn currentTurn;
	boolean lastChance = false;
	Kitty roundKitty = new Kitty();
	
	/*
	 * Score at which a round is considered done
	 */
	final int WINNING_SCORE = 12;
	
	/*
	 * Index of the currently active player
	 */
	int activePlayerIndex = 0;
	/*
	 * Index of the first player to reach the winning score for the round
	 */
	int firstWinnerIndex = -1;

	Round(ArrayList<Player> players) {
		this.players = players;
	}

	private Player findWinner() {
		int max = 0;
		Player winner = players.get(0);
		for (int i = 0; i < players.size(); i++) {
			if(players.get(i).getRoundScore() > max) {
				winner = players.get(i);
				firstWinnerIndex = i;
				max = winner.getRoundScore();
			}
		}		
		return winner;
	}
	
	private void moveChips(Player winner) {
		// winner gets kitty chips
		winner.setChipCount(winner.getChipCount() + roundKitty.getChipCount());

		// winner collects chips from other players
		for (int i = 0; i < players.size(); i++) {
			if (players.get(i) != winner) {
				if (players.get(i).getRoundScore() == 0) {
					players.get(i).setChipCount(players.get(i).getChipCount() - 10);
					winner.setChipCount(winner.getChipCount() + 10);
				} else {
					players.get(i).setChipCount(players.get(i).getChipCount() - 5);
					winner.setChipCount(winner.getChipCount() + 5);
				}
			}
		}
	}

	/*
	 * Plays one more turn for each player until the turn is done or input is required
	 * 
	 */
	public ResultSummary playLastChance(String input) {
		ResultSummary response;
		if (activePlayerIndex != firstWinnerIndex) {
			Player active = players.get(activePlayerIndex);
			response = playTurnStep(active, input);
		} else {
			 //last chances done!
			response = new ResultSummary();
			response.setNextState(State.DONE);
			Player winner = findWinner();
			moveChips(winner);
			response.setWinnerName(winner.getName());
			response.setWinningChipCount(winner.getChipCount());
			response.setWinningScore(winner.getRoundScore());
			response.setPlayers(players);
		}

		return response;
	}

	public ResultSummary playTurnStep(String input) {
		Player active = players.get(activePlayerIndex);				
		ResultSummary response = playTurnStep(active, input);
		response.setPlayerName(active.getName());		
		return response;
	}
	
	/*
	 * Plays a turn for the active player until the turn is done or input is required
	 * 
	 */
	private ResultSummary playTurnStep(Player activePlayer, String input) {
		ResultSummary response = new ResultSummary();
		response.setNextState(State.PLAYING_TURN);		
		if(currentTurn == null) {
			currentTurn = new Turn();
		}
		
		int turnScore = currentTurn.getTurnScore();				
		
		if(input == null) {			
			response.setNextState(State.WAITING_FOR_INPUT);
		}		
		else if (input.equals("y")) {
			currentTurn.rollAgain();
			currentTurn.scoreTurn();
			response.setLastRoll(currentTurn.getLastRoll());
			
			turnScore = currentTurn.getTurnScore();
			if (currentTurn.ends()) {
				response.setNextState(State.TURN_DONE);
			} else {
				response.setNextState(State.WAITING_FOR_INPUT);
			}
		} else if (input.equals("n")) {
			response.setNextState(State.TURN_DONE);
		} else {
			response.setNextState(State.INVALID_RESPONSE);
		}						
		
		if(response.getNextState() == State.TURN_DONE) {
			if((currentTurn.getLastRoll() != null) 
					&& (currentTurn.getLastRoll().isDoubleSkunk())) {
				activePlayer.setRoundScore(0);
			} else {
				activePlayer.setRoundScore(activePlayer.getRoundScore() + turnScore);
			}
			activePlayer.setChipCount(activePlayer.getChipCount() + currentTurn.getChipChange());
			roundKitty.setChipCount(roundKitty.getChipCount() + currentTurn.getKittyChange());
			currentTurn = null;
			if(!lastChance) {
				checkForWinner();
			}
		}
		response.setPlayerName(activePlayer.getName());
		response.setRoundScore(activePlayer.getRoundScore());
		response.setTurnScore(turnScore);
				
		return response;
	}

	private void checkForWinner() {
		if (players.get(activePlayerIndex).getRoundScore() >= WINNING_SCORE) {
			firstWinnerIndex = activePlayerIndex;
			lastChance = true;
		}
	}
	
	public boolean roundDone() {
		for (int i = 0; i < players.size(); i++) {
			if (players.get(i).getRoundScore() >= WINNING_SCORE) {				
				return true;
			}
		}
		return false;
	}
	
	public boolean lastChanceDone() {
		if( activePlayerIndex == firstWinnerIndex) {
			return true;
		}
		return false;
	}

	public ResultSummary updateActivePlayer() {
		ResultSummary response = new ResultSummary();
		activePlayerIndex += 1;
		if (activePlayerIndex > players.size() - 1) {
			activePlayerIndex = 0;
		}	
		if (!roundDone()) {
			response.setNextState(State.PLAYING_TURN);
		} else if (lastChance == true && !lastChanceDone()){
			response.setNextState(State.LAST_CHANCE);
		} else {
			response = new ResultSummary();
			response.setNextState(State.DONE);
			Player winner = findWinner();
			moveChips(winner);
			response.setWinnerName(winner.getName());
			response.setWinningChipCount(winner.getChipCount());
			response.setWinningScore(winner.getRoundScore());
			response.setPlayers(players);			
		}
		response.setPlayerName(this.players.get(activePlayerIndex).getName());
		return response;
	}

}
