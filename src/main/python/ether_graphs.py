#!/usr/bin/python3

import sys
import random
import math
import matplotlib
import matplotlib.pyplot as plt
import numpy as np
from matplotlib.widgets import Slider
from datetime import datetime

PHI = (1 + 5 ** 0.5) / 2
#Elements: Earth, Water, AIr, Fire
#ELEMENTAL_RATIOS = [(PHI),(PHI + 1/PHI),(PHI + 1/PHI + 1/(PHI ** 2)),(PHI + 1/PHI + 1/(PHI ** 2) + 1/(PHI ** 3))]
ELEMENTAL_RATIOS = [2,3,4,5]

class Cell:
    aether = float(random.uniform(0,10)) #stationary
    nether = float(random.uniform(0,10)) #aviary
    target_ratio = PHI
    requested_aether = 0
    requested_nether = 0
    available_aether = 0
    available_nether = 0

    def __init__(self):
        random.seed(datetime.now())
        self.aether = float(random.uniform(0,10))
        self.nether = float(random.uniform(0,10))
        self.requested_aether = 0
        self.requested_nether = 0
        self.available_aether = 0
        self.available_nether = 0
        self.target_ratio = self.get_target_ratio()

    def ratio(self):
        return float(self.nether / max(self.aether,0.000000000000001))

    def phi_delta(self):
        return float(self.ratio()) - float(self.target_ratio)

    def printMe(self):
        print( "%.2f; %.2f; %.4f ==> %.4f " % (self.aether, self.nether, self.ratio(), self.target_ratio) )

    def get_target_ratio(self):
        if self.ratio() <= (ELEMENTAL_RATIOS[0] + ELEMENTAL_RATIOS[1])/2:
            return ELEMENTAL_RATIOS[0]
        elif self.ratio() <= (ELEMENTAL_RATIOS[1] + ELEMENTAL_RATIOS[2])/2:
            return ELEMENTAL_RATIOS[1]
        elif self.ratio() <= (ELEMENTAL_RATIOS[2] + ELEMENTAL_RATIOS[3])/2:
            return ELEMENTAL_RATIOS[2]
        else:
            return ELEMENTAL_RATIOS[3]

    def get_ether_step(self, requested, available):
        if(0 < requested):
            if(requested < available):
                return requested
            else:
                return available
        else:
            return requested
    def get_target_aether(self):
        return (self.nether / (self.target_ratio + (nether_dynamic*self.phi_delta())))

    def get_target_nether(self):
        return self.aether * (self.target_ratio + (1.0 - nether_dynamic)*(self.phi_delta()))

    def step_pre_process(self):
        if(not math.isclose(self.target_ratio,self.ratio())):
            #decide target ratio
            self.target_ratio = self.get_target_ratio()

            #calculate the requested ether
            self.requested_aether = self.get_target_aether() - self.aether
            self.requested_nether = self.get_target_nether() - self.nether

            #calculate requested values for the polarities
            self.aether += self.requested_aether
            self.available_aether -= self.requested_aether
            self.nether += self.requested_nether
            self.available_nether -= self.requested_nether

    def step(self, nether_dynamic):
        if(not math.isclose(self.target_ratio,self.ratio())):

            #restore ether values to before the pre-process stage
            self.aether -= self.requested_aether
            self.available_aether += self.requested_aether
            self.nether -= self.requested_nether
            self.available_nether += self.requested_nether

            #step in the direction of the target ratio
            tmp_ether = self.get_ether_step(self.requested_aether,self.available_aether)
            self.aether += tmp_ether
            self.available_aether -= tmp_ether
            tmp_ether = self.get_ether_step(self.requested_nether,self.available_nether)
            self.nether += tmp_ether
            self.available_nether -= tmp_ether

            #Equalize available polarity values to 0
            self.aether += self.available_aether * 0.5
            self.available_aether -= self.available_aether
            self.nether += self.available_nether * 0.5
            self.available_nether -= self.available_nether

# Arguments #
# [0] the file name for whatever reason
# [1] target ratio
# [2] Nether dynamic

cell = []
ae_plot = []
ne_plot = []
ratio_plot = []
target_ratio_plot = []

nether_dynamic = 0.6

fig, axs = plt.subplots(6, 3) #rows, cols
nether_dynamic_slider = Slider(plt.axes([0.25, 0.15, 0.65, 0.03]), "nether dynamic", 0,1.5, valinit=nether_dynamic)
plt.subplots_adjust(left=0.1,bottom=0.35)
ratio_y_ax = [-ELEMENTAL_RATIOS[3],-ELEMENTAL_RATIOS[2],-ELEMENTAL_RATIOS[1],-ELEMENTAL_RATIOS[0],0.0, ELEMENTAL_RATIOS[0],ELEMENTAL_RATIOS[1],ELEMENTAL_RATIOS[2],ELEMENTAL_RATIOS[3]]

def init():
    global cell
    global ae_plot
    global ne_plot
    global ratio_plot
    global target_ratio_plot
    cell = []
    ae_plot = []
    ne_plot = []
    ratio_plot = []
    target_ratio_plot = []
    for x in [0,1,2]:
        cell.append([])
        ae_plot.append([])
        ne_plot.append([])
        ratio_plot.append([])
        target_ratio_plot.append([])
        for y in [0,1,2]:
            cell[x].append(Cell())
            ae_plot[x].append([])
            ne_plot[x].append([])
            ratio_plot[x].append([])
            target_ratio_plot[x].append([])
            ae_plot[x][y].append(cell[x][y].aether)
            ne_plot[x][y].append(cell[x][y].nether)
            ratio_plot[x][y].append(cell[x][y].ratio())
            target_ratio_plot[x][y].append(cell[x][y].target_ratio)

def calculate_shared_ether(): #average the available reserves in neighbouring cells so sharing is possible
    global cell
    averages_aae = [[0,0,0],[0,0,0],[0,0,0]]
    averages_ane = [[0,0,0],[0,0,0],[0,0,0]]
    for x in [0,1,2]:
        for y in [0,1,2]:
            x_range = [ele for ele in list(range(x-1,x+2)) if (ele >= 0 and ele <= 2)]
            y_range = [ele for ele in list(range(y-1,y+2)) if (ele >= 0 and ele <= 2)]
            for inx in x_range:
                for iny in y_range:
                    averages_aae[x][y] += cell[inx][iny].available_aether
                    averages_ane[x][y] += cell[inx][iny].available_nether
            averages_aae[x][y] /= 9
            averages_ane[x][y] /= 9
            cell[x][y].available_aether = averages_aae[x][y]
            cell[x][y].available_nether = averages_ane[x][y]

def pre_process_cell(x,y):
    global cell
    cell[x][y].step_pre_process()

def re_calculate_cell(x,y):
    global cell
    cell[x][y].step(nether_dynamic)


def re_calculate():
    global ae_plot
    global ne_plot
    global ratio_plot
    global target_ratio_plot
    init()

    watchdog = 0
    #while (not math.isclose(cell[x][y].ratio(),cell[x][y].target_ratio, abs_tol=1e-4) and (watchdog < 500)):
    while(watchdog < 10):
        for x in [0,1,2]:
            for y in [0,1,2]:
                pre_process_cell(x,y)
        calculate_shared_ether()
        for x in [0,1,2]:
            for y in [0,1,2]:
                re_calculate_cell(x,y)
                ae_plot[x][y].append(cell[x][y].aether)
                ne_plot[x][y].append(cell[x][y].nether)
                ratio_plot[x][y].append(cell[x][y].ratio())
                target_ratio_plot[x][y].append(cell[x][y].target_ratio)

        watchdog += 1
    update_plots()

def update(val):
    global nether_dynamic
    nether_dynamic = nether_dynamic_slider.val
    re_calculate()

def main():
    re_calculate()
    plt.show()

def update_plots():
    global fig
    global axs
    for x in [0,1,2]:
        for y in [0,1,2]:
            axs[y*2 + 0][x].clear()
            axs[y*2 + 1][x].clear()
            axs[y*2 + 0][x].plot(ae_plot[x][y], label="Æther", color="blue")
            axs[y*2 + 0][x].plot(ne_plot[x][y], label = "Nether", color="red")
            axs[y*2 + 1][x].text(0, 0, "(%i,%i)" % (x,y) , ha='center', va='center', size=12, alpha=.5)
            axs[y*2 + 1][x].set_yticks(ratio_y_ax)
            axs[y*2 + 1][x].plot(ratio_plot[x][y], color="lightblue")
            axs[y*2 + 1][x].plot(target_ratio_plot[x][y], color="orange")
    fig.canvas.draw()
    fig.canvas.flush_events()

nether_dynamic_slider.on_changed(update)

if __name__ == '__main__':
    main()
