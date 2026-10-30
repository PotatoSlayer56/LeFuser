package potatoslayer56.lefuser;

public class Shard {
  public String name;
  public String id;
  public String bazaarID;
  public float buyOrderPrice;
  public float sellOrderPrice;
  public boolean isReptile;
  public int fusionAmount;

  public Shard(String n, String i, String bzID, float bOP, float sOP, boolean rep, int amt){
    this.name = n;
    this.id = i;
    this.bazaarID = bzID;
    this.buyOrderPrice = bOP;
    this.sellOrderPrice = sOP;
    this.isReptile = rep;
    this.fusionAmount = amt;
  }
}
