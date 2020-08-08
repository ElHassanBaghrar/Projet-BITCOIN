package neo4j;

import info.blockchain.api.APIException;
import info.blockchain.api.blockexplorer.BlockExplorer;
import info.blockchain.api.blockexplorer.entity.Block;
import info.blockchain.api.blockexplorer.entity.Input;
import info.blockchain.api.blockexplorer.entity.Output;
import org.neo4j.driver.*;

import java.io.IOException;
import java.util.List;

public class GraphManager implements AutoCloseable
{


    private final Driver driver;

    public GraphManager( String uri, String user, String password )
    {
        driver = GraphDatabase.driver( uri, AuthTokens.basic( user, password ) );
    }

    @Override
    public void close() throws Exception
    {
        driver.close();

    }


    public void insertBloc(int debut, int fin) throws APIException, IOException {


        Session session1 = driver.session();
        Session session2 = driver.session();
        Session session3 = driver.session();
        Session session4 = driver.session();
        Session session5 = driver.session();
        Session session6 = driver.session();
        BlockExplorer blockExplorer = new BlockExplorer();

        for (int j = debut; j < fin; j++)
            try {
                {
                    Block block = blockExplorer.getBlock(j);

                    try (Transaction txNEO4J = session1.beginTransaction()) {
                        txNEO4J.run("MERGE (block:block {hash:'" + block.getHash() + "'})\n" +
                                "SET block.prevblock='" + block.getPreviousBlockHash() + "', \n" +
                                "block.time='" + block.getTimeHuman() + "', \n" +
                                "block.num='" + j + "', \n" +
                                "block.txcount='" + block.getTransactions().size() + "' \n" +
                                "MERGE (prevblock:block { hash:'" + block.getPreviousBlockHash() + "'}) MERGE (block)-[:chain]->(prevblock)");
                        txNEO4J.commit();

                    }

                    System.out.println("Les infos du bloc " + j + "==> OK");
                    List<info.blockchain.api.blockexplorer.entity.Transaction> txBloc = block.getTransactions();
                    for (int t = 0; t < block.getTransactions().size(); t++)
                     {
                        List<Input> inputs = txBloc.get(t).getInputs();
                        List<Output> outputs = txBloc.get(t).getOutputs();
                        try (Transaction txNEO4J = session2.beginTransaction())
                        {
                            txNEO4J.run("MATCH (block :block {hash:'" + block.getHash() + "'})\n" +
                                    "MERGE (tx:tx {txid:'" + txBloc.get(t).getHash() + "'})\n" +
                                    "MERGE (tx)-[:inc {i:'" + t + "'}]->(block)");
                            txNEO4J.commit();
                        }

                        for (Input in : inputs)
                        {

                            if (inputs.isEmpty() || inputs.get(0).getPreviousOutput() == null)
                            {
                                String node_in = "COINBASE";
                                try (Transaction txNEO4J = session3.beginTransaction())
                                {
                                    txNEO4J.run("MERGE (tx:tx { txid :'" + txBloc.get(t).getHash() + "'}) \n " +
                                            "CREATE (value_in:value_in { value :'" + node_in + "' }) \n" +
                                            "MERGE (value_in) -[:value_in {in :'" + inputs.indexOf(in) + "' }]->(tx)");
                                    txNEO4J.commit();
                                }
                                continue;
                            } else
                                {
                                try (Transaction txNEO4J = session4.beginTransaction())
                                {

                                    txNEO4J.run("MERGE (tx:tx { txid :'" + txBloc.get(t).getHash() + "'}) \n " +
                                            "CREATE(value_in:value_in {value :'" + in.getPreviousOutput().getValueBTC() + "' }) \n" +
                                            "MERGE (value_in) -[:in {in :'" + inputs.indexOf(in) + "' }]->(tx) \n" +
                                            "MERGE (address_in:address_in {addressid_in :'" + in.getPreviousOutput().getAddress() + "'}) \n" +

                                            "MERGE (value_in) -[:unlocked_by {unlocked_by :'" + in.getPreviousOutput().getAddress() + "' }] ->(address_in)");
                                    txNEO4J.commit();
                                }
                            }
                        }

                        for (Output out : outputs)
                        {

                            if (outputs.isEmpty() || outputs.get(0).getAddress() == null)
                            {
                                String node_out = "NOADDRESS";
                                try (Transaction txNEO4J = session5.beginTransaction())
                                {
                                    txNEO4J.run("MERGE (tx:tx { txid :'" + txBloc.get(t).getHash() + "'}) \n " +
                                            "MERGE(value_out:value_out { value :'" + node_out + "' }) \n" +
                                            "MERGE (tx) -[:out {out :'" + outputs.indexOf(out) + "' }]->(value_out)");
                                    txNEO4J.commit();
                                }

                                continue;

                            } else {
                                try (Transaction txNEO4J = session6.beginTransaction())
                                {
                                    txNEO4J.run("MERGE (tx:tx { txid :'" + txBloc.get(t).getHash() + "'}) \n " +
                                            "CREATE (value_out:value_out {value :'" + out.getValueBTC() + "' }) \n" +
                                            "MERGE (tx) -[:out {out :'" + outputs.indexOf(out) + "' }]->(value_out) \n" +
                                            "MERGE  (address_out:address_out {addressid_out :'" + out.getAddress() + "'}) \n" +
                                            "MERGE (value_out) -[:locked_by {locked_by :'" + out.getValueBTC() + "' }] ->(address_out)");
                                    txNEO4J.commit();
                                }


                            }


                        }


                        System.out.println("Inputs et outputs num :  " + t + " sur " + block.getTransactions().size() + " ==> OK ");


                    }


                    System.out.println("le bloc num : " + j + " a été entièrement inséré !");


                }
            }
            catch (Exception e )
            {
                System.out.println("Bloc num : " + j + "non inserés");
            }
            finally
            {
                continue;
            }
    }





}


