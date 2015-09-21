package networkLayer;

class node { 
		public Double distance = Double.POSITIVE_INFINITY;
		public String nextNode = "";
		public String destination = "";

		public node() {}
		public node(Double newDist, String newNext, String newDest) {
			distance = newDist;
			nextNode = newNext;
			destination = newDest;
		}
};