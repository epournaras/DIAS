/*
 * Copyright (C) 2015 Evangelos Pournaras
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package protocols;

import dsutil.protopeer.services.aggregation.AggregationFunction;
import aggregation.AggregationState;
import communication.DIASMessType;
import consistency.AggregationOutcome;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import peerlets.measurements.MeasurementTags;
import protopeer.measurement.LogReplayer;
import protopeer.measurement.MeasurementLog;
import protopeer.measurement.MeasurementLoggerListener;

/**
 *
 * @author Evangelos
 */
public class DIASLogReplayer {

    private final static String expSeqNum="01";
    private final static String expID="LossExperiment"+expSeqNum+"/";

    private LogReplayer replayer;
    private final String coma=",";


    public DIASLogReplayer(String logsDir, int minLoad, int maxLoad){
        this.replayer=new LogReplayer();
        this.loadLogs(logsDir, minLoad, maxLoad);
        this.replayResults();
    }

    public static void main(String args[]){
    	System.out.println("Reading from folder "+args[0]);
        DIASLogReplayer replayer=new DIASLogReplayer(args[0], 0, 1000);
    }

    public void loadLogs(String directory, int minLoad, int maxLoad){
        try{
            File folder = new File(directory);
            File[] listOfFiles = folder.listFiles();
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()&&!listOfFiles[i].isHidden()) {
                    MeasurementLog loadedLog=replayer.loadLogFromFile(directory+"/"+listOfFiles[i].getName());
                    //System.err.println(loadedLog.toString());
                    MeasurementLog replayedLog=this.getMemorySupportedLog(loadedLog, minLoad, maxLoad);
                    replayer.mergeLog(replayedLog);
                }
                else
                    if (listOfFiles[i].isDirectory()) {
                        //do sth else
                    }
            }
        }
        catch(IOException io){

        }
        catch(ClassNotFoundException ex){

        }
    }

    public void replayResults(){
//        this.printGlobalMetricsTags();
//        this.calculatePeerResults(replayer.getCompleteLog());
        this.printLocalMetricsTags();
        replayer.replayTo(new MeasurementLoggerListener(){
            public void measurementEpochEnded(MeasurementLog log, int epochNumber){
                calculateEpochResults(log, epochNumber);
            }
        });
    }

    private void calculatePeerResults(MeasurementLog globalLog){
        
    }

    private void calculateEpochResults(MeasurementLog log, int epochNumber){
        double epochNum=epochNumber;
        double avgAggregationEpoch=log.getAggregateByEpochNumber(epochNumber, MeasurementTags.EPOCH).getAverage();
        double minAggregationEpoch=log.getAggregateByEpochNumber(epochNumber, MeasurementTags.EPOCH).getMin();
        double maxAggregationEpoch=log.getAggregateByEpochNumber(epochNumber, MeasurementTags.EPOCH).getMax();
        double numOfPushes=log.getAggregateByEpochNumber(epochNumber, DIASMessType.PUSH).getSum();
        double numOfPullPushes=log.getAggregateByEpochNumber(epochNumber, DIASMessType.PULL_PUSH).getSum();
        double numOfPulls=log.getAggregateByEpochNumber(epochNumber, DIASMessType.PULL).getSum();
        double firstOutcomes=log.getAggregateByEpochNumber(epochNumber, AggregationOutcome.FIRST).getSum();
        double doubleOutcomes=log.getAggregateByEpochNumber(epochNumber, AggregationOutcome.DOUBLE).getSum();
        double replaceOutcomes=log.getAggregateByEpochNumber(epochNumber, AggregationOutcome.REPLACE).getSum();
        double unsuccessfulOutcomes=log.getAggregateByEpochNumber(epochNumber, AggregationOutcome.UNSUCCESSFUL).getSum();
        double amdCounter=log.getAggregateByEpochNumber(epochNumber, MeasurementTags.AMD_COUNTER).getAverage();
        double smaCounter=log.getAggregateByEpochNumber(epochNumber, MeasurementTags.SMA_COUNTER).getAverage();
        double dmaCounter=log.getAggregateByEpochNumber(epochNumber, MeasurementTags.DMA_COUNTER).getAverage();
        double amsCounter=log.getAggregateByEpochNumber(epochNumber, MeasurementTags.AMS_COUNTER).getAverage();
        double amdFPP=log.getAggregateByEpochNumber(epochNumber, MeasurementTags.AMD_FP).getAverage();
        double smaFPP=log.getAggregateByEpochNumber(epochNumber, MeasurementTags.SMA_FP).getAverage();
        double dmaFPP=log.getAggregateByEpochNumber(epochNumber, MeasurementTags.DMA_FP).getAverage();
        double amsFPP=log.getAggregateByEpochNumber(epochNumber, MeasurementTags.AMS_FP).getAverage();
//        double numOfExploited=log.getAggregateByEpochNumber(epochNumber, MeasurementTags.EXPLOITED_SIZE).getAverage();
//        double numOfUnexploited=log.getAggregateByEpochNumber(epochNumber, MeasurementTags.UNEXPLOITED_SIZE).getAverage();
//        double numOfOutdated=log.getAggregateByEpochNumber(epochNumber, MeasurementTags.OUTDATED_SIZE).getAverage();
        double avgEstim=log.getAggregateByEpochNumber(epochNumber, AggregationFunction.AVG).getAverage();
        double sumEstim=log.getAggregateByEpochNumber(epochNumber, AggregationFunction.SUM).getAverage();
        double sumSqrEstim=log.getAggregateByEpochNumber(epochNumber, AggregationFunction.SUM_SQR).getAverage();
        double maxEstim=log.getAggregateByEpochNumber(epochNumber, AggregationFunction.MAX).getAverage();
        double minEstim=log.getAggregateByEpochNumber(epochNumber, AggregationFunction.MIN).getAverage();
        double stDevEstim=log.getAggregateByEpochNumber(epochNumber, AggregationFunction.STDEV).getAverage();
        double countEstim=log.getAggregateByEpochNumber(epochNumber, AggregationFunction.COUNT).getAverage();
        double avgActual=log.getAggregateByEpochNumber(epochNumber, "SELECTION").getAverage();
        double sumActual=log.getAggregateByEpochNumber(epochNumber, "SELECTION").getSum();
        double sumSqrActual=log.getAggregateByEpochNumber(epochNumber, "SELECTION").getSumSquared();
        double maxActual=log.getAggregateByEpochNumber(epochNumber, "SELECTION").getMax();
        double minActual=log.getAggregateByEpochNumber(epochNumber, "SELECTION").getMin();
        double stDevActual=log.getAggregateByEpochNumber(epochNumber, "SELECTION").getStdDev();
        double countActual=log.getAggregateByEpochNumber(epochNumber, "SELECTION").getNumValues();
        System.out.println(epochNum+coma+avgAggregationEpoch+coma+minAggregationEpoch+coma+maxAggregationEpoch+coma+numOfPushes+coma+numOfPullPushes+coma+numOfPulls+coma+
                firstOutcomes+coma+doubleOutcomes+coma+replaceOutcomes+coma+unsuccessfulOutcomes+coma+
                amdCounter+coma+smaCounter+coma+dmaCounter+coma+amsCounter+coma+amdFPP+coma+smaFPP+coma+dmaFPP+coma+amsFPP+coma+
//                numOfExploited+coma+numOfUnexploited+coma+numOfOutdated+coma+
                avgEstim+coma+sumEstim+coma+sumSqrEstim+coma+maxEstim+coma+minEstim+coma+stDevEstim+coma+countEstim+coma+
                avgActual+coma+sumActual+coma+sumSqrActual+coma+maxActual+coma+minActual+coma+stDevActual+coma+countActual);
        
    }

    private MeasurementLog getMemorySupportedLog(MeasurementLog log, int minLoad, int maxLoad){
        return log.getSubLog(minLoad, maxLoad);
    }

    public void printGlobalMetricsTags(){
       System.out.println("*** RESULTS PER PEER ***\n");
    }

    public void printLocalMetricsTags(){
        System.out.println("*** RESULTS PER EPOCH ***\n");
        System.out.println("# of Epoch,# of Aggregation Epoch,Min. Aggregation Epoch,Max Aggregation Epoch,# of Push Mess.,# of Pull-Push Mess.,# of Pull Mess.," +
                "# of Outcome=FIRST,# of Outcomes=DOUBLE,# of Outcomes=REPLACE,# of Outcomes=UNSUCCESSFUL," +
//                "# of Exploited,# of Unexploited,# of Outdated,"+
                "AMD Counter,SMA Counter,DMA Counter,AMS Counter,AMD F.P. Prob.,SMA F.P. Prob.,DMA F.P. Prob.,AMS F.P. Prob.," +
                "Avegare (Estimated),Sum (Estimated),Sum Square (Estimated),Max (Estimated),Min (Estimated),Stand. Deviation (Estimated),Count (Estimated)," +
                "Avegare (Actual),Sum (Actual),Sum Square (Actual),Max (Actual),Min (Actual),Stand. Deviation (Actual),Count (Actual)");
    }

    public double roundDecimals(double decimal, int decimalPlace) {
        BigDecimal bd = new BigDecimal(decimal);
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_UP);
        return bd.doubleValue();
    }

}
