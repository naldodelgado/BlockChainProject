package org.example.Auction;

import java.util.logging.Logger;
import java.util.HashMap;
import java.util.TreeSet;

public class AuctionsState {

    private static final HashMap<String, AuctionState> auctionStates = new HashMap<>();
    private static final HashMap<String, Long> walletsTrans = new HashMap<>(); //state of how much money wallets have spent on Auctions
    private static final Logger logger = Logger.getLogger(AuctionState.class.getName());

    
    public static boolean addAuction(Auction auction){
        if(!auction.verifyAuction()){
            return false;
        }
        if(auctionStates.containsKey(auction.get_idItem())){
            logger.warning("auction already added");
            return false;
        }
        
        /*AuctionState newAuctionState =  new AuctionState(auction);
        auctionStates.put(auction.get_idItem(), newAuctionState);
        logger.info("A new auction was added");*/
        return true;
    }


    class AuctionState{ 
        Auction auction;
        //TreeSet<Bid> bids = new TreeSet<>(new BidCompare());


        public AuctionState(Auction auction) {
            this.auction = auction;
        }
    

        public Auction getAuction() {
            return auction;
        }

        /*public TreeSet<Bid> getBids() {
            return bids;
        }*/


        public void printAuctionState(){
            System.out.println("Auction : " + auction.get_idItem());
            //Bid latest = this.getLatestBid();
            //if (latest==null){
            if(true){
                System.out.println("No latest bid");
            }
            else{
                //System.out.println("Latest Bid is off" + latest.getAmount());
            }
            System.out.println();
        }

    }


}
