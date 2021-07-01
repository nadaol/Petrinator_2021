from abc import ABC, abstractmethod


class costManager(ABC):

    @abstractmethod
    def transitionFired(self, transition, cost):
        pass

    @abstractmethod
    def getBeta(self, cluster):
        pass
