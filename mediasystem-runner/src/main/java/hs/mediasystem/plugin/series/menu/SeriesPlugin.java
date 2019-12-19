package hs.mediasystem.plugin.series.menu;

import hs.mediasystem.plugin.rootmenu.MenuPresentation.Menu;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.MenuItem;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.Plugin;
import hs.mediasystem.runner.collection.CollectionDefinition;
import hs.mediasystem.runner.collection.CollectionLocationManager;
import hs.mediasystem.runner.util.ResourceManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SeriesPlugin implements Plugin {
  @Inject private SeriesCollectionType collectionType;
  @Inject private CollectionLocationManager manager;

  @Override
  public Menu getMenu() {
    List<MenuItem> menuItems = new ArrayList<>();

    for(CollectionDefinition collectionDefinition : manager.getCollectionDefinitions("Serie")) {
      menuItems.add(new MenuItem(collectionDefinition.getTitle(), null, () -> collectionType.createPresentation(collectionDefinition.getTag())));
    }

    return new Menu("Series", ResourceManager.getImage(getClass(), "image"), menuItems);
  }
}
