# BLOCKCHAIN
In the context of the Systems and Data Security course, part of the
Master's Degree in Network and Systems Engineering at the Faculty of Sciences of the University of Porto.
University of Porto, we were given the challenging project of implementing a Public
Ledger Auctions. This project consists of creating a blockchain (without permissions) with the aim of developing a public ledger.
with the aim of developing a decentralised public ledger capable of storing auction transactions.
transactions in a secure and transparent manner. In this report, we will detail the choices
choices made in the implementation process and the difficulties faced in development.


# [Project Description](https://cdn-uploads.piazza.com/paste/itcshcp58zg2wx/25f90c5bf25c1e5ba7ed42e0f5f2684a0b8c8849e8018ab99bfc4064a976ea5a/assigment.pdf)

The project aims to design and build a decentralised public ledger specifically adapted to
to store auction transactions. The work is divided into three main parts
main parts:
- _*A secure p2p network (Kademlia)*_
- _*A distributed ledger (Blockchain)*_
- _*A system for auctions with wallets and transactions (Auctions)*_

## P2P Network

Kademlia is a structured peer-to-peer distribute hash table (DHT) system which has several advantages compared to protocols like Chord [18] as a results of using a novel XOR metric for distance between points in the identifier space. Because XOR is a symmetric operation, Kademlia nodes receive lookup queries from the same nodes which are also in their local routing tables. This is important in order that nodes can learn useful routing information from lookup queries and update their routing tables. In contrast Chord needs a dedicated stabilization protocol due to the asymmetric nature of the overlay topology. The XOR metric is also unidirectional: For any constant x there exists exactly one y which has a distance of d(x,y). This ensures that lookups for the same key converge along the same path independently of the origination which is an important property for caching mechanisms. For more information check:
   * [kademlia by Petar Maymounkov and David Mazières](https://pdos.csail.mit.edu/~petar/papers/maymounkov-kademlia-lncs.pdf)
   * [Kademlia on IEEExplore](https://ieeexplore.ieee.org/document/4447808)
     
### Our implementation of Kademlia

We used gRPC to implement Kademlia, following the normal procedures for implementing services in gRPC.
procedures for implementing services in gRPC. Our .proto file consists of the following services
of the following services:

* `rpc Ping (Sender) returns (Node) {}`
* `rpc StoreBlock (kBlock) returns (Node) {}`
* `rpc StoreTransaction (kTransaction) returns (Node) {}`
* `rpc FindNode (KeyWithSender) returns (KBucket) {}`
* `rpc FindBlock (KeyWithSender) returns (BlockOrKBucket) {}`
* `rpc FindTransaction (TransactionKey) returns (TransactionOrBucket) {}`
* `rpc HasTransaction (TransactionKey) returns (Boolean) {}`
* `rpc HasBlock (KeyWithSender) returns (Boolean) {}`

Each N has an ID with 160 bits. For an object to be sent in kademlia,
it must have a 160-bit hash. The network has 3 objects: Blocks, Auctions and Bids
These objects are shared via kademlia for all N nodes.

Each Node has a binary number that represents its distance from another Node.
This distance is calculated by XORing each bit of the Node IDs.
Kademlia is based on Kbuckets that store at least one N number K. Each
Kbucket stores N ́os with the same number of zeros at the beginning. For example
for example, if one N ́o has a distance of 0001110 and another has a distance of 0001001,
then they will be stored in the same kbucket.
This principle is used to store the network in such a way that each N is accessible with
at most log(n) of hops.

## Public Ledger ( _*BlockChain*_ )

To develop a blockchain, it was essential to first understand the concept of this
technology. A blockchain is nothing more than a chain or list of blocks, where each block is represented by a hash and contains the hash of the previous block.
block is represented by a hash and contains the hash of the previous block, as well as the data.
If the data in a block is changed, the hash of the block will be modified, affecting
all the hashes of subsequent blocks[3][4]. This mechanism guarantees the integrity
integrity of the blockchain, since any attempt to alter a block would be immediately detected, thus preserving reliability.
detected, thus preserving the reliability and security of the network.

The mining process in our network begins with an initial encoded node that
 is responsible for mining the genesis block. In the subsequent network, we opted for a
division between the nodes: miners and users, the latter who will carry out transactions in the future.
in the future. Each miner waits for a block to be received before starting the mining process.
process. Meanwhile, each user has a copy of the blockchain, but only
miners have permission to add new blocks to it. In other words,
when a user creates a new block, they transmit it to all the miners on the
network. The first miner to mine successfully broadcasts the new block to all the
users on the network, who then add it to their blockchain accounts.
This approachguarantees the integrity and decentralisation of the network, with users participating in the process of validating
process of validating transactions through the miners.

### Our implementation of the Public Ledger ( _*BlockChain*_ )

It needs to be established that a transaction can consist of two things: An auction or
a bid. In our Block Chain implementation, the intention is to support
buyers and sellers so that they can maintain an immutable history of transactions.
of transactions. Whenever a client creates a request, the blockchain layer creates a
data structure ``MAP < Auction, List < Wallet >>`` that subscribes the customer to this auction so that they are notified whenever a bid is created.
bid on that auction.

Each Block is built after the successful propagation of a minimum number of transactions.
of transactions is achieved. When this requirement is met, the mining process begins.
process begins in which an attempt is made to find a nonce that when hashed together with the rest of the information
with the rest of the relevant information in the block (timestamp, hash of the previous block and
merkle root from the list of transactions) satisfies the minimum number of zeros required in the
hash for the block to be considered valid.

A validated, newly mined block is then propagated through the rest of the network so that it can be added to the chain.
it can be validated by the rest of the network and then added to the chain. When a
node receives a block that has been mined by another node through the network and is correctly validated, it is then
is correctly validated, then the receiving node for any mining process that is in progress
to then resume the process of harvesting transactions and building new blocks,
since the transactions present in the received block are already present in a block.
This method minimises the probability of extended forking. Extended Forking
 is the phenomenon that usually occurs when two blocks are mined and propagated
even if they have the same predecessor number. This should not happen. To solve this
solve this problem, we chose the branch of the blockchain whose first block has the
hash with the largest binary equivalent

A dificuldade de minera ̧c ̃ao  ́e ajustada mudando o n ́umero m ́ınimo de zeros necess ́arios
para que uma nonce seja considerada v ́alida. Esse processo  ́e concebido de seguinte
forma: Se os  ́ultimos 10 blocos foram minerados em um tempo menor do que o es-
perado ent ̃ao o n ́umero m ́ınimo requerido  ́e aumentado. Esse n ́umero  ́e decrementado
se o tempo de minera ̧c ̃ao dos  ́ultimos 10 blocos for menor que aquele esperado. Uma
tentativa de ajuste de dificuldade  ́e efetuado sempre que um bloco  ́e minerado por um
bloco ou  ́e recebido por um N ́o atrav ́es da rede.

## Auctions

The purpose this ledger is to provide support to a platform where buyers and sellers can publish auctions or post bids on auctions and keep the records immutable. It is important to mention that we do not validade each Auction/Bid in order to make the action valid. We hash them so the record of the action is immutable. This could be implemented to provide a deeper level of security and integrity of the network. Along with proof of stake we could implement a cryptocurrency system where we could penalize each node that perjures the network

Similarly to what happens with cryptocurrencies, ownership of a currency is transferred on the blockchain by means of transactions.
transactions. All the participants in the blockchain have an address to which they can send their transactions.
We implemented a class called Wallet, which contains two important fields:
privatekey and publicKey. The public key (publicKey) of a blockchain participant
acts as its address and can be shared with other participants to receive payments.
receive payments. In turn, the private key is used to digitally sign transactions.
transactions, guaranteeing the authenticity and security of the operations carried out.

We used electric curve cryptography to generate these keys. We opted for
ECDSA _*(Elliptic Curve Digital Signature Algorithm)*_, a choice in line with the algorithm used by Bitcoin.
with the algorithm used by Bitcoin. This method offers a high level of security and efficiency.
and efficiency, and has been widely adopted in several blockchain implementations due to its robustness and reliability.
its robustness and reliability, essential characteristics to guarantee integrity and security.
security of transactions.
The transactions created are represented by the winning bids in an auction, involving the payment of the winner of the auction.
the payment from the winner of the auction to the creator of the auction[2][3]. We use
bouncycastle as the security provider for our project.

## Improvement opportunities

Our implementation of the Public Ledger did not implement a [proof of stake](https://www.investopedia.com/terms/p/proof-stake-pos.asp) system. This means that we are subcetible to certain atacks:

* *Sybil attacks*
   - This type of attack occurs when an attacker takes advantage of the ease of creating new
   new identities and creates several pseudonyms to fool the reputation system of a network.
   of a network. If an attacker wanted to direct this type of attack at a
   blockchain, they would flood the network with new nodes and connect to honest participants in the
   participants in the network in order to provide them with false information.
* *Eclipse attacks*
  - This type of attack involves selecting an endpoint and directing all communication flows from it to a specific node.
   communication flows to a specific node on the network, thus cutting off all communication.
   it makes or receives from other nodes. A successful attack results in
   a distorted view of the blockchain by the victim.
   By isolating a portion of the competing miners from the blockchain, an attacker
   is able to remove their processing power (hashpower), thus allowing the attacker to
   the attacker to control a greater percentage of the total hashpower. This can
   This can lead to a significant increase in the attacker's power on the network, thus jeopardising
   the integrity and security of the blockchain.



