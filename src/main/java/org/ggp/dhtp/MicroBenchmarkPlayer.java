package org.ggp.dhtp;
import java.util.List;
import java.util.Random;

import org.ggp.base.apps.player.Player;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.dhtp.propnet.InternalMachineState;
import org.ggp.dhtp.propnet.PropWyattStateMachine;

public class MicroBenchmarkPlayer extends StateMachineGamer {

	Player p;
	boolean USE_PROPNET = false;
	boolean USE_WYATT = true;
	boolean USE_CACHE = false;

	@Override
	public PropWyattStateMachine getInitialStateMachine() {
		// TODO Auto-generated method stub
		/*
		StateMachine rawMachine;
		if(USE_PROPNET){
			rawMachine = new SamplePropNetStateMachine();

		} else {
			rawMachine = new ProverStateMachine();
		}
		if(USE_CACHE){
			return new CachedStateMachine(rawMachine);
		} else {
			return rawMachine;
		}
		*/
		return new PropWyattStateMachine();
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// TODO Auto-generated method stub

	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long turnTime = timeout - System.currentTimeMillis();
		long limit = (long)(turnTime*0.75) + System.currentTimeMillis();
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		if(machine instanceof PropWyattStateMachine){
			state = ((PropWyattStateMachine)machine).convertToInternal(state);
		}
		Role role = getRole();
		System.out.println("Getting legal moves with state " + ((InternalMachineState)state).getBitSet());
		List<Move> moves = machine.getLegalMoves(state,role);
		System.out.println("Got legal moves " + moves);
		int randomIndex = new Random().nextInt(moves.size());
		long mctsStart = System.currentTimeMillis();
		int numDepthCharges=0;
		while(System.currentTimeMillis() < limit){
			machine.performDepthCharge(state, new int[1]);
			numDepthCharges++;
		}
		long mctsMs = System.currentTimeMillis() - mctsStart;
		double dcps = 1000.0*((double)numDepthCharges)/mctsMs;
		System.out.print("DCPS: "+dcps+"\n");
		return moves.get(randomIndex);
	}

	@Override
	public void stateMachineStop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stateMachineAbort() {
		// TODO Auto-generated method stub

	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Don't hate the microbenchmark player";
	}

}
