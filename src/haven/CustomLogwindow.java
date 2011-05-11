package haven;

import java.awt.Color;
import java.awt.font.TextAttribute;
import java.util.*;
import java.util.Map.Entry;

public class CustomLogWindow extends CustomWindow {
	private CustomTextlog tl;
	private HashMap<String, ToggleButton> tabs;
	private HashMap<String, LogManager.Log> logs;
	private Button clearbtn;
	private String currenttab;
	
	public CustomLogWindow(Coord c, Coord sz, Widget parent, String cap) {
	    super(c, sz, parent, cap);
	    
	    tabs = new HashMap<String, ToggleButton>();
	    logs = new HashMap<String, LogManager.Log>();
	    // text log
	    tl = new CustomTextlog(new Coord(0, 25), new Coord(0, 0), this);
	    tl.drawbg = false;
	    tl.defcolor = Color.WHITE;
	    // clear button
	    clearbtn = new Button(new Coord(0, 0), 40, this, "Clear") {
	    	public void click() {
	    		clear();
	    	}
	    };
	    pack();
    }
	
	public void addlog(String name, LogManager.Log log) {
		// add tab button
		ToggleButton tb = new ToggleButton(new Coord(tabs.size() * 85, 0), 80, this, name) {
			public void click() {
			    settab(this);
			}
	    };
	    tabs.put(name, tb);
	    logs.put(name, log);
	    // activate first added tab
	    if (tabs.size() == 1)
	    	settab(tb);
	    pack();
	}
	
	private void clear() {
		if (currenttab == null)
			return;
		logs.get(currenttab).clear();
	}
	
	@Override
	protected synchronized Coord getminsz() {
		int tabwidth = 0;
		if (tabs != null) {
			tabwidth = tabs.size() * 85;				
		}
		int minwidth = Math.max(tabwidth, 40);
		return new Coord(minwidth + this.mrgn.x * 2, 125);
	}
	
	private void settab(ToggleButton tabbtn) {
		synchronized(tabs) {
			for (Entry<String, ToggleButton> entry : tabs.entrySet()) {
				String tab = entry.getKey();
				ToggleButton btn = entry.getValue();
				btn.settoggle(btn == tabbtn);
				if (btn == tabbtn) {
					currenttab = tab;
					tl.setlog(logs.get(tab));
				}
			}
		}
	}
	
	@Override
	protected void recalcsz(Coord max) {
		super.recalcsz(max);
		if (tl != null) {
			tl.sz = this.asz.sub(0, 55);
		}
		if (clearbtn != null) {
			clearbtn.c = new Coord(0, this.asz.y - 25);
		}
	}
	
    @Override	
    public boolean mousedown(Coord c, int button) {
    	boolean b = super.mousedown(c, button);
    	if (rsm)
    		tl.beginupdate(); // stop text rendering
    	return b;
    }
    
    @Override
    public boolean mouseup(Coord c, int button) {
    	if (rsm)
    		tl.endupdate();
    	return super.mouseup(c, button);
    }
	
    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == cbtn) {
		this.hide();
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }
    
    @Override
    public boolean type(char key, java.awt.event.KeyEvent ev) {
	if(key == 27) {
		this.hide();
	    return(true);
	}
	return(super.type(key, ev));
    }
	
	private static class CustomTextlog extends Textlog implements LogManager.LogView {
		static RichText.Foundry fnd = new RichText.Foundry(TextAttribute.FAMILY, "Sans Serif", TextAttribute.SIZE, 10, TextAttribute.FOREGROUND, Color.WHITE);
		
		private boolean update = false;
		private LogManager.Log log = null;

		public CustomTextlog(Coord c, Coord sz, Widget parent) {
	        super(c, sz, parent);
        }
		
		@Override
		public void append(String line, Color col) {
			Text rl;
			if(col == null)
			    col = defcolor;
			line = RichText.Parser.quote(line);
			rl = fnd.render(line, sz.x - (margin * 2) - sflarp.sz().x);
			synchronized(lines) {
			    lines.add(rl);
			}
			if(cury == maxy)
			    cury += rl.sz().y;
			maxy += rl.sz().y;
		}
		
		public void removefirst() {
			synchronized(lines) {
			    if (lines.size() == 0)
			    	return;
				Text rl = lines.get(0);
				lines.remove(0);
				maxy -= rl.sz().y;
				if (cury > maxy)
					cury = maxy;
			}
		}
		
		public void clear() {
			lines.clear();
			update();
		}
		
		public void beginupdate() {
			update = true;
		}
		
		public void endupdate() {
			update();
			update = false;
		}
		
	    @Override
	    public void draw(GOut g) {
	    	if (!update)
	    		super.draw(g);
	    	else {
	    		g.chcolor(new Color(0, 0, 0, 120));
	    		g.frect(new Coord(0, 0), sz.sub(margin + sflarp.sz().x, 0));
	    	}
	    }
	    
	    public void setlog(LogManager.Log log) {
	    	if (this.log != null)
	    		this.log.setview(null);
	    	if (this.log == log)
	    		return;
	    	this.log = log;
	    	beginupdate();
	    	lines.clear();
	    	for (String line : this.log.lines())
	    		append(line);
	    	endupdate();
	    	this.log.setview(this);	    		
	    }
	    
	    private void update() {
			Text[] oldlines = new Text[lines.size()];
			oldlines = lines.toArray(oldlines);
			synchronized(lines) {
				lines.clear();
				cury = 0;
				maxy = 0;
				for (Text line : oldlines) {
					Text rl = fnd.render(line.text, sz.x - (margin * 2) - sflarp.sz().x);
				    lines.add(rl);
					if(cury == maxy)
					    cury += rl.sz().y;
					maxy += rl.sz().y;
				}
			}
	    }
	}
}
