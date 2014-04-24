package luca.tmac.basic.obligations;

public interface ObligationMonitorable {
	
	public void notifyDeadline(Obligation obl);
	
	public void notifyFulfillment(Obligation obl);

	void notifyObligationInsert(Obligation obl);
}
