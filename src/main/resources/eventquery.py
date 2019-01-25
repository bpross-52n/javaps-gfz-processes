import pandas
import obspy
import obspyDMT
import sys


#DUMMY DATA STUFF SHOULD BE CHANGED AS SOON AS STORAGE ETC IS FINALLY DECIDED
#FIXME:currently only csv
def read_database(conn):
    '''
    get data from database
    '''
    return pandas.read_csv(conn)

def connect(provider='GFZ'):
    '''
    connects to service
    '''
    if provider=='GFZ':
        return read_database("/home/bpr/example_event_db.csv")

#FUNCTIONS
def convert_360(lon):
    '''
    convert a longitude specified with 180+
    '''
    return lon-360

def filter_spatial(db,lonmin=-180,lonmax=180,latmin=-90,latmax=90,zmin=0,zmax=999):
    '''
    filters spatial
    '''
    return db[(db.longitude >= lonmin) & (db.longitude >= lonmin) & (db.latitude >= latmin) & (db.latitude <= latmax) & (db.depth >= zmin) & (db.depth <= zmax)]

def filter_type(db,etype,probability):
    '''
    filters event type and probability
    '''
    if etype in ['historic','stochastic','historic','deaggregation']:
        return db[(db.type==etype) & (db.probability > p)]
    elif etype in ['expert']:
        return db[(db.type==etype) & (db.probability==p)]

def filter_magnitude(db,mmin,mmax):
    '''
    filters magnitude
    '''
    return db[(db.magnitude >= mmin) & (db.magnitude >= mmax)]

def events2quakeml(events):
    '''
    Returns quakeml form pandas event set
    '''
    pass

#QUERY
def query_events(db,lonmin=-180,lonmax=180,latmin=-90,latmax=90,mmin=0,mmax=9,zmin=0,zmax=999,p=0,etype='historic'):
    '''
    Returns set of events
    type can be:
        -stochastic (returns stochastic set of events, probability is rate of event)
        -expert     (returns expert defined events, probability is rate of event)
        -psha       (returns events matching ground motion for psha at target, given probability of exceedance)
        -deaggregation (returns events matching deaggregation, probability is lower level of exceedance probability)

    Optional Constraints
        - target: tlat,tlon
        - distance:
        - boundary region: lonmin,lonmax,latmin,latmax (default:-180,180,-90,90)
        - minimum magnitude: mmin (Mw, default:0)
        - maximum depth: zmax (km, default 999)
        - probability: p (interpretation depends on type see above)
    '''

    print(sys.argv)

    #filter type and probability
    selected = filter_type(db,etype,p)

    #print(lonmin)

    #convert 360 degree longitude in case
    if lonmin > 180:
        lonmin = convert_360(lonmin)
    if lonmax > 180:
        lonmax = convert_360(lonmax)

    #print(lonmax)

    #spatial filter
    selected = filter_spatial(selected,lonmin,lonmax,latmin,latmax,zmin,zmax)

    #print(selected)

    #magnitude filter
    selected = filter_magnitude(db,mmin,mmax)

    selected.to_csv(path_or_buf='/tmp/selected.csv')    

    #print(selected)

    #TODO convert to quakeml
    #selected=events2quakeml(selected)

    #print(selected)

    return selected

#Program execution

db = connect()

#test query params
lonmin=float(sys.argv[1])
lonmax=float(sys.argv[2])
latmin=float(sys.argv[3])
latmax=float(sys.argv[4])
mmin=float(sys.argv[5])
mmax=float(sys.argv[6])
zmin=float(sys.argv[7])
zmax=float(sys.argv[8])
p=float(sys.argv[9])
etype=sys.argv[10]
#etype='deaggregation'
#etype='stochastic'
#etype='expert'
#poe='likely',

selected = query_events(db,lonmin,lonmax,latmin,latmax,mmin,mmax,zmin,zmax,p,etype)
