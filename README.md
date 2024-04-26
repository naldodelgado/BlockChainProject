# BLOCKCHAIN
### We are going to add some enlightenning description and guides here temporarely

In a typical blockchain, a single block can indeed contain multiple bids. Bundling multiple bids into a single block
allows for more efficient processing and validation of bids within the network. This batch processing helps in
optimizing resource utilization and improving overall system performance.

By including multiple bids in a block, miners can compete to find a valid block hash that incorporates all of these
bids. Once a miner successfully mines a block, all the bids within that block are considered confirmed and added to the
blockchain, providing a reliable record of bid history.

The ability to include multiple bids in a block is a key feature of blockchain technology, enabling scalability and
facilitating the smooth operation of decentralized systems. However, it's essential to strike a balance between block
size, bid throughput, and network performance to ensure the blockchain remains efficient and sustainable over time.

### Encryption in Blockchain

The decision of where to apply encryption within a blockchain system depends on your specific use case and security
requirements. Both encrypting bids before adding them to a block and encrypting the entire block itself have their
advantages, and the choice depends on what you aim to achieve with encryption.

Here are some considerations for both approaches:

1. **Encrypting Transactions Before Adding to a Block**:
   - Encrypting individual bids can provide privacy and confidentiality for the bid data.
   - It ensures that sensitive information within each bid, such as sender addresses, recipient addresses, and bid
     amounts, remains confidential.
   - Encrypting bids at this level allows for selective disclosure of bid details to authorized parties, providing
     fine-grained control over data visibility.

2. **Encrypting the Entire Block**:
    - Encrypting the entire block adds another layer of security to the blockchain data.
   - It protects the integrity and confidentiality of all bids within the block, as well as the block metadata.
    - Encrypting blocks can prevent unauthorized access or tampering with the block content, ensuring that only authorized parties can view or modify the block data.

3. **Bouncy Castle for Encryption**:
    - Bouncy Castle is a popular cryptographic library that provides a wide range of cryptographic algorithms and functionalities.
   - It can be used for both encrypting bids before adding them to a block and encrypting the entire block itself.
    - Bouncy Castle's encryption capabilities can be utilized based on your specific encryption requirements and preferences.

### Addressing Nonces in the Block and Transaction Structures 
When deciding on encryption within a blockchain system, it's essential to consider the trade-offs between security, performance, and complexity. Encryption adds computational overhead and can impact system performance, so it's crucial to balance security needs with practical considerations.
If your primary concern is protecting the confidentiality of bid details, encrypting bids before adding them to a block
might be more appropriate. On the other hand, if you want to ensure the overall security and integrity of the blockchain
data, encrypting the entire block could be a better option.

Regarding the usage of Bouncy Castle for encryption, it can be used for both encrypting bids and encrypting blocks, as
it provides a wide range of cryptographic algorithms and functionalities. You can utilize Bouncy Castle's encryption
capabilities based on your specific encryption requirements and preferences.

Remember that encryption adds computational overhead and complexity to the system, so you should consider the performance implications and balance them with your security needs. Additionally, ensure that you properly manage encryption keys and access controls to maintain data confidentiality and integrity effectively.

The nonce in the Block structure is indeed used for mining and proof-of-work (PoW) consensus algorithms. Miners adjust the nonce value in attempts to find a valid block hash that satisfies the difficulty criteria imposed by the PoW algorithm.

On the other hand, the Transaction structure typically does not include a nonce field. Transactions are created by users
to transfer assets (cryptocurrencies or tokens) from one address to another. They are not directly involved in the
mining process or the proof-of-work algorithm. Instead, each bid has a unique identifier (bid ID) and may include other
fields such as sender address, recipient address, amount, signature, etc.

Therefore, you should include the nonce field only in the Block data structure for mining purposes. Here's the corrected version of the Block class without the nonce field in the Transaction structure:

### todo

- [ ] add a mechanism to download the blockchain
- [X] deal with extended forking of the blockchain
- [ ] add a mechanism to update the nodes in the route table in kadmelia
- [ ] process to update the mining difficulty
- [ ] How far back does the extended forking go?
- [ ] Should I deal with cloned bids?
- [ ] Can I validate that a block is sent to all the nodes in a kbucket by querying it after some time
- [ ] Validate requests