package juno.smarthome;

import ail.mas.DefaultEnvironment;
import ail.mas.MAS;
import ail.mas.scheduling.RoundRobinScheduler;
import ail.syntax.Predicate;
import ajpf.MCAPLJobber;

public class FireEnv extends DefaultEnvironment implements MCAPLJobber {
	final static String logname = "juno.smarthome.FireEnv";
	static int DAY = 0;
	static int EVENING = 1;
	static int NIGHT = 2;
	int time = DAY;
	static int FIRE = 0;
	int fires = 0;
	
	public FireEnv() {
		super();
		RoundRobinScheduler scheduler = new RoundRobinScheduler();
		this.setScheduler(scheduler);
		addPerceptListener(scheduler);
		
		getScheduler().addJobber(this);
	}

	@Override
	public int compareTo(MCAPLJobber o) {
		return o.getName().compareTo(getName());
	}

	@Override
	public void do_job() {
		if (time == DAY) {
			time = EVENING;
			removePercept(new Predicate("day"));
			addPercept(new Predicate("evening"));
			System.err.println("Evening");
			if (fires == 1) {
				FIRE = 0;
				removePercept(new Predicate("fire"));
				fires++;
			}
		} else if (time == EVENING) {
			time = NIGHT;
			removePercept(new Predicate("evening"));
			addPercept(new Predicate("night"));		
			System.err.println("Night");
			if (fires == 0) {
				fires = 1;
			}
			if (fires == 2) {
				FIRE = 1;
				System.err.println("Fire!");
				addPercept(new Predicate("fire"));				
			}
		} else {
			time = DAY;
			removePercept(new Predicate("night"));
			addPercept(new Predicate("day"));
			System.err.println("Day");
			if (fires == 1) {
				FIRE = 1;
				System.err.println("Fire!");
				addPercept(new Predicate("fire"));
			}
		}
	}

	@Override
	public String getName() {
		return logname;
	}
	
	@Override
	public void setMAS(MAS m) {
		super.setMAS(m);
		addPercept(new Predicate("day"));
	}

}
