/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2016  Universidade da Beira Interior (UBI, Portugal) and
 *     the Instituto Federal de Educação Ciência e Tecnologia do Tocantins (IFTO, Brazil).
 *
 *     This file is part of CloudSim Plus.
 *
 *     CloudSim Plus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     CloudSim Plus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with CloudSim Plus. If not, see <http://www.gnu.org/licenses/>.
 */
package org.cloudsimplus.examples;

import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristicsSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.util.Log;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerHeuristic;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.distributions.UniformDistr;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudsimplus.heuristics.CloudletToVmMappingHeuristic;
import org.cloudsimplus.heuristics.CloudletToVmMappingSimulatedAnnealing;
import org.cloudsimplus.heuristics.CloudletToVmMappingSolution;
import org.cloudsimplus.heuristics.HeuristicSolution;

/**
 * An example that uses a
 * <a href="http://en.wikipedia.org/wiki/Simulated_annealing">Simulated Annealing</a>
 * heuristic to find a suboptimal mapping between Cloudlets and Vm's submitted to a
 * DatacenterBroker. The number of {@link Pe}s of Vm's and Cloudlets are defined
 * randomly.
 *
 * <p>The {@link DatacenterBrokerHeuristic} is used
 * with the {@link CloudletToVmMappingSimulatedAnnealing} class
 * in order to find an acceptable solution with a high
 * {@link HeuristicSolution#getFitness() fitness value}.</p>
 *
 * <p>Different {@link CloudletToVmMappingHeuristic} implementations can be used
 * with the {@link DatacenterBrokerHeuristic} class.</p>
 *
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Plus 1.0
 */
public class DatacenterBrokerHeuristicExample {
    private final CloudSim simulation;
    private List<Cloudlet> cloudletList;
    private List<Vm> vmList;
    private CloudletToVmMappingSimulatedAnnealing heuristic;

    /**
     * Number of cloudlets created so far.
     */
    private int numberOfCreatedCloudlets = 0;
    /**
     * Number of VMs created so far.
     */
    private int numberOfCreatedVms = 0;
    /**
     * Number of hosts created so far.
     */
    private int numberOfCreatedHosts = 0;

    private static final int HOSTS_TO_CREATE = 100;
    private static final int VMS_TO_CREATE = 50;
    private static final int CLOUDLETS_TO_CREATE = 100;

	/**
	 * Simulated Annealing (SA) parameters.
	 */
	public static final double SA_INITIAL_TEMPERATURE = 1.0;
	public static final double SA_COLD_TEMPERATURE = 0.0001;
	public static final double SA_COOLING_RATE = 0.003;
	public static final int    SA_NUMBER_OF_NEIGHBORHOOD_SEARCHES = 50;

    /**
     * Starts the simulation.
     * @param args
     */
    public static void main(String[] args) {
        new DatacenterBrokerHeuristicExample();
    }

    /**
     * Default constructor where the simulation is built.
     */
    public DatacenterBrokerHeuristicExample() {
        Log.printFormattedLine("Starting %s ...", getClass().getSimpleName());
        this.vmList = new ArrayList<>();
        this.cloudletList = new ArrayList<>();

        simulation = new CloudSim();

        Datacenter datacenter0 = createDatacenter();

        DatacenterBrokerHeuristic broker0 = createBroker();

        createAndSubmitVms(broker0);
        createAndSubmitCloudlets(broker0);

        simulation.start();

        List<Cloudlet> finishedCloudlets = broker0.getCloudletFinishedList();
        new CloudletsTableBuilder(finishedCloudlets).build();

        print(broker0);
    }

	private DatacenterBrokerHeuristic createBroker() {
		createSimulatedAnnealingHeuristic();
		DatacenterBrokerHeuristic broker0 = new DatacenterBrokerHeuristic(simulation);
		broker0.setHeuristic(heuristic);
		return broker0;
	}

	private void createAndSubmitCloudlets(DatacenterBrokerHeuristic broker0) {
		for(int i = 0; i < CLOUDLETS_TO_CREATE; i++){
		    cloudletList.add(createCloudlet(broker0, getRandomNumberOfPes(4)));
		}
		broker0.submitCloudletList(cloudletList);
	}

	private void createAndSubmitVms(DatacenterBrokerHeuristic broker0) {
		vmList = new ArrayList<>(VMS_TO_CREATE);
		for(int i = 0; i < VMS_TO_CREATE; i++){
		    vmList.add(createVm(broker0, getRandomNumberOfPes(4)));
		}
		broker0.submitVmList(vmList);
	}

	private void createSimulatedAnnealingHeuristic() {
		heuristic =
		        new CloudletToVmMappingSimulatedAnnealing(SA_INITIAL_TEMPERATURE, new UniformDistr(0, 1));
		heuristic.setColdTemperature(SA_COLD_TEMPERATURE);
		heuristic.setCoolingRate(SA_COOLING_RATE);
		heuristic.setNumberOfNeighborhoodSearchesByIteration(SA_NUMBER_OF_NEIGHBORHOOD_SEARCHES);
	}

	private void print(DatacenterBrokerHeuristic broker0) {
		double roudRobinMappingCost = computeRoudRobinMappingCost();
		printSolution(
		        "Heuristic solution for mapping cloudlets to Vm's         ",
		        heuristic.getBestSolutionSoFar(), false);

		System.out.printf(
		    "The heuristic solution cost represents %.2f%% of the round robin mapping cost used by the DatacenterBrokerSimple\n",
		    heuristic.getBestSolutionSoFar().getCost()*100.0/roudRobinMappingCost);
		System.out.printf("The solution finding spend %.2f seconds to finish\n", broker0.getHeuristic().getSolveTime());
		System.out.println("Simulated Annealing Parameters");
		System.out.printf("\tInitial Temperature: %.2f", SA_INITIAL_TEMPERATURE);
		System.out.printf(" Cooling Rate: %.4f", SA_COOLING_RATE);
		System.out.printf(" Cold Temperature: %.6f", SA_COLD_TEMPERATURE);
		System.out.printf(" Number of neighborhood searches by iteration: %d\n", SA_NUMBER_OF_NEIGHBORHOOD_SEARCHES);
		Log.printFormattedLine("\n%s finished!", getClass().getSimpleName());
	}

	/**
     * Randomly gets a number of PEs (CPU cores).
     *
     * @param maxPesNumber the maximum value to get a random number of PEs
     * @return the randomly generated PEs number
     */
    private int getRandomNumberOfPes(int maxPesNumber) {
        return heuristic.getRandomValue(maxPesNumber)+1;
    }

    private DatacenterSimple createDatacenter() {
        List<Host> hostList = new ArrayList<>();
        for(int i = 0; i < HOSTS_TO_CREATE; i++) {
            hostList.add(createHost());
        }

        //Defines the characteristics of the data center
        double cost = 3.0; // the cost of using processing in this Datacenter
        double costPerMem = 0.05; // the cost of using memory in this Datacenter
        double costPerStorage = 0.001; // the cost of using storage in this Datacenter
        double costPerBw = 0.0; // the cost of using bw in this Datacenter

        DatacenterCharacteristics characteristics =
            new DatacenterCharacteristicsSimple(hostList)
                .setCostPerSecond(cost)
                .setCostPerMem(costPerMem)
                .setCostPerStorage(costPerStorage)
                .setCostPerBw(costPerBw);

        return new DatacenterSimple(simulation, characteristics, new VmAllocationPolicySimple());
    }

    private Host createHost() {
        long mips = 1000; // capacity of each CPU core (in Million Instructions per Second)
        int  ram = 2048; // host memory (MEGABYTE)
        long storage = 1000000; // host storage
        long bw = 10000;

        List<Pe> peList = new ArrayList<>();
        /*Creates the Host's CPU cores and defines the provisioner
        used to allocate each core for requesting VMs.*/
        for(int i = 0; i < 8; i++)
            peList.add(new PeSimple(mips, new PeProvisionerSimple()));

       return new HostSimple(ram, bw, storage, peList)
           .setRamProvisioner(new ResourceProvisionerSimple())
           .setBwProvisioner(new ResourceProvisionerSimple())
            .setVmScheduler(new VmSchedulerTimeShared());
    }

    private Vm createVm(DatacenterBroker broker, int pesNumber) {
        long mips = 1000;
        long   storage = 10000; // vm image size (MEGABYTE)
        int    ram = 512; // vm memory (MEGABYTE)
        long   bw = 1000; // vm bandwidth

        return new VmSimple(numberOfCreatedVms++, mips, pesNumber)
            .setRam(ram).setBw(bw).setSize(storage)
            .setCloudletScheduler(new CloudletSchedulerTimeShared());

    }

    private Cloudlet createCloudlet(DatacenterBroker broker, int numberOfPes) {
        long length = 400000; //in Million Structions (MI)
        long fileSize = 300; //Size (in bytes) before execution
        long outputSize = 300; //Size (in bytes) after execution

        //Defines how CPU, RAM and Bandwidth resources are used
        //Sets the same utilization model for all these resources.
        UtilizationModel utilization = new UtilizationModelFull();

        return new CloudletSimple(numberOfCreatedCloudlets++, length, numberOfPes)
        .setFileSize(fileSize)
        .setOutputSize(outputSize)
        .setUtilizationModel(utilization);
    }

    private double computeRoudRobinMappingCost() {
        CloudletToVmMappingSolution roudRobinSolution =
                new CloudletToVmMappingSolution(heuristic);
        int i = 0;
        for (Cloudlet c : cloudletList) {
            //cyclically selects a Vm (as in a circular queue)
            roudRobinSolution.bindCloudletToVm(c, vmList.get(i));
            i = (i+1) % vmList.size();
        }
        printSolution(
            "Round robin solution used by DatacenterBrokerSimple class",
            roudRobinSolution, false);
        return roudRobinSolution.getCost();
    }

    private void printSolution(String title,
            CloudletToVmMappingSolution solution,
            boolean showIndividualCloudletFitness) {
        System.out.printf("%s (cost %.2f fitness %.6f)\n",
                title, solution.getCost(), solution.getFitness());
        if(!showIndividualCloudletFitness)
            return;

        for(Map.Entry<Cloudlet, Vm> e: solution.getResult().entrySet()){
            System.out.printf(
                "Cloudlet %3d (%d PEs, %6d MI) mapped to Vm %3d (%d PEs, %6.0f MIPS)\n",
                e.getKey().getId(),
                e.getKey().getNumberOfPes(), e.getKey().getLength(),
                e.getValue().getId(),
                e.getValue().getNumberOfPes(), e.getValue().getMips());
        }
        System.out.println();
    }

}
