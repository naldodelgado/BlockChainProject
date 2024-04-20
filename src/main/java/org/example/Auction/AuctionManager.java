package org.example.Auction;

import org.example.Wallet;

import java.security.SecureRandom;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Logger;

public class AuctionManager {

    private Auction auction;
    private Wallet seller;
    //private TreeSet<Bid> bids_status;
    private Thread runningAuction;
    private static final Logger  logger = Logger.getLogger(AuctionManager.class.getName());
    private static final Scanner scanner =new Scanner(System.in);


    public AuctionManager(Wallet seller) {
        this.seller = seller;
        this.auction = getAuction(seller);
        //AuctionsState.addAuction(auction);
        //this.bids_status = AuctionsState.getAuctionBidsTreeSet(auction.get_idItem());
       // 
        //runningAuction = new Thread(this, "AuctionRunning: " + auction.get_idItem());
        runningAuction.start();

    }

    public AuctionManager(Auction auction) {
        this.auction = auction;
        //AutionState.addAuction(auction);
        //this.bids_status= AuctionsState.getAuctionBidsTreeSet(auction.get_idItem());
    }

    /*private Transaction createTransaction(Bid winBid) {
        return new Transaction(seller, winBid);
    }*/

    //@Override
    public void run() {
        logger.info("started auction");
        /*while(this.bids_status.isEmpty()){
            this.bids_status = AuctionsState.getAuctionBidsTreeSet(auction.get_idItem());
            try {
                sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Bid winBid = this.bids_status.last();
        long timestamp = new Date().getTime();
        while((new Date().getTime())-timestamp<this.auction.getTimeout()){
            if (this.bids_status.last()!=winBid){
                winBid=this.bids_status.last();
                timestamp = new Date().getTime();
            }else{
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        logger.info("Auction has ended");
        Transaction trans = createTransaction(winBid);
        */
    }


    private Auction getAuction(Wallet seller) {
        //System.out.println("What do you wish the auctionID to be:");
        byte[] randomID = new byte[6];
        Random random = new SecureRandom();
        random.nextBytes(randomID);

        String name = Utils.bytesToHexString(randomID) ;
        
        System.out.println("Insert the minimun amount for the bid");
        long amountRead = scanner.nextLong();
        
        System.out.println("Insert the minimun increment percentage");
        long increment = scanner.nextLong();
       
        System.out.println("Insert the fee for the auction \nBigger fees equals faster processing");
        long fee = scanner.nextLong();
        
        System.out.println("Insert the maximum time between bets in seconds");
        long timeout=scanner.nextLong() * 1000;
        
        Auction auction = new Auction(name, amountRead, increment, fee, timeout, seller);
        return auction;
    }

    public Auction getAuction() {
        return auction;
    }

    public Thread getRunningAuction() {
        return runningAuction;
    }

    
}
