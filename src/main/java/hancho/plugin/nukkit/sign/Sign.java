package hancho.plugin.nukkit.sign;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.SignChangeEvent;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.response.FormResponseModal;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.level.Location;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class Sign extends PluginBase implements Listener {
    public static final SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd");
    public static final String PREFIX = "§f[ §b! §f] ";
    public static final int MAIN_FORM = 128512;
    public static final int CHECK_SIGN_FORM = MAIN_FORM  + 1;
    //public static final int LIST_SIGN_FORM = MAIN_FORM  + 2; //simple, 돌아가기 버튼
    public HashMap<String, ArrayList<String>> data;
    public HashMap<String, HashSet<String>> signedPlayer;
    public HashMap<String, String> owner;
    public HashMap<String, String> queue = new HashMap<>();

    public static String locationToStringKey(Location location){
        return new StringBuilder().append(location.getLevel().getName()).append("_").append(location.getFloorX()).append("_").append(location.getFloorY()).append("_").append(location.getFloorZ()).toString();
    }

  /*  public static Location stringToLocation(String string){
        String[] strings = string.split("_");
        return new Location(Double.parseDouble())
    }*/

    @Override
    public void onEnable() {
        LinkedHashMap<String, Object> dataMap = (LinkedHashMap<String, Object>) new Config(this.getDataFolder().getAbsolutePath() + "/data.yml", Config.YAML).getAll();
        LinkedHashMap<String, Object> signedPlayerMap = (LinkedHashMap<String, Object>) new Config(this.getDataFolder().getAbsolutePath() + "/signedPlayer.yml", Config.YAML).getAll();
        LinkedHashMap<String, Object> ownerMap = (LinkedHashMap<String, Object>) new Config(this.getDataFolder().getAbsolutePath() + "/owner.yml", Config.YAML).getAll();
        this.data = new LinkedHashMap(dataMap);
        this.signedPlayer = new LinkedHashMap(signedPlayerMap);
        this.owner = new LinkedHashMap(ownerMap);
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        Config dataConfig = new Config(this.getDataFolder().getAbsolutePath() + "/data.yml", Config.YAML);
        Config signedPlayerConfig = new Config(this.getDataFolder().getAbsolutePath() + "/signedPlayer.yml", Config.YAML);
        Config ownerConfig = new Config(this.getDataFolder().getAbsolutePath() + "/owner.yml", Config.YAML);  //혹시 모를 대비용
        LinkedHashMap dataMap = new LinkedHashMap(this.data);
        LinkedHashMap signedPlayerMap = new LinkedHashMap(this.signedPlayer);
        LinkedHashMap ownerMap = new LinkedHashMap(this.owner);
        dataConfig.setAll(dataMap);
        signedPlayerConfig.setAll(signedPlayerMap);
        ownerConfig.setAll(ownerMap);
        dataConfig.save();
        signedPlayerConfig.save();
        ownerConfig.save();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent ev){
        if(ev.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        Block block = ev.getBlock();
        if(block.getId() != BlockID.SIGN_POST && block.getId() != BlockID.WALL_SIGN) return;
        String key = locationToStringKey(ev.getBlock().getLocation());
        if(data.containsKey(key)){
            this.showMainForm(ev.getPlayer());
            this.queue.put(ev.getPlayer().getName(), key);
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent ev){
        if(ev.getLine(0).equals("사인")) {
            ev.setLine(0, "§f[ §b사인 §f]");
            ev.setLine(1, "§f총 §b0§f명이 사인했습니다");
            ev.setLine(3, "§f터치해보세요!");
            String key = locationToStringKey(ev.getBlock().getLocation());
            this.data.put(key, new ArrayList<>());
            this.signedPlayer.put(key, new HashSet<>());
            this.owner.put(key, ev.getPlayer().getName());
        }
    }

    @EventHandler
    public void onRespond(PlayerFormRespondedEvent ev) {
        if (ev.getWindow() == null) return;
        if (ev.getResponse() == null) return;
        Player player = ev.getPlayer();
        String name = player.getName();
        int id = ev.getFormID();
        if (ev.getWindow() instanceof FormWindowSimple) {
            FormWindowSimple window = (FormWindowSimple) ev.getWindow();
            FormResponseSimple response = window.getResponse();
            int clickedId = response.getClickedButtonId();
            if(id == MAIN_FORM){
                if(clickedId == 0){ //서명
                    this.showCheckSignForm(player);
                }else if(clickedId == 1){ //목록
                    this.showSignList(player);
                }
                return;
            }
        } else if (ev.getWindow() instanceof FormWindowModal) {
            FormWindowModal window = (FormWindowModal) ev.getWindow();
            FormResponseModal response = window.getResponse();
            int clickedId = response.getClickedButtonId();
            if(id == CHECK_SIGN_FORM){
                if(clickedId == 0){
                    String key = this.queue.get(player.getName());
                    if(this.signedPlayer.get(key).contains(player.getName())){
                        FormWindowSimple form = new FormWindowSimple("§0서명", PREFIX + "이미 서명하셨습니다!" );
                        player.showFormWindow(form);
                        return;
                    }
                    ArrayList<String> list = this.data.get(key);
                    String time = sdf.format(System.currentTimeMillis());
                    list.add(player.getName() + "_" + time);
                    this.data.put(key, list);
                    HashSet<String> set = this.signedPlayer.get(key);
                    set.add(player.getName());
                    this.signedPlayer.put(key, set);

                    String[] strings = key.split("_");
                    FormWindowSimple form = new FormWindowSimple("§0서명", PREFIX + "사인했습니다.\n" + PREFIX + "서명한 시각 : §o" + time);
                    player.showFormWindow(form);

                    Location location = new Location(Double.parseDouble(strings[1]), Double.parseDouble(strings[2]), Double.parseDouble(strings[3]), this.getServer().getLevelByName(strings[0]));
                    BlockEntitySign sign = (BlockEntitySign) location.level.getBlockEntity(location);
                    String[] texts = sign.getText();
                    sign.setText(texts[0], "§f총 §b" + list.size() + "§f명이 사인했습니다", texts[2], texts[3]);

                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent ev){
        Block block = ev.getBlock();
        if(block.getId() != BlockID.SIGN_POST && block.getId() != BlockID.WALL_SIGN) return;
        String key = locationToStringKey(ev.getBlock().getLocation());
        if(this.data.containsKey(key)){
            this.data.remove(key);
            this.owner.remove(key);
            this.signedPlayer.remove(key);
        }
    }

    public void showMainForm(Player player){
        ArrayList<ElementButton> buttons = new ArrayList<>();
        buttons.add(new ElementButton("§l§0사인하기\n§r사인을 해보세요!"));
        buttons.add(new ElementButton("§0§l목록\n§r사인들을 확인해보세요"));
        FormWindowSimple form = new FormWindowSimple("§0서명", PREFIX + "사인을 남겨서 자신의 흔적을 남겨보세요!\n" + PREFIX + "§r§f표지판에 사인을 적어 직접 만들 수 있습니다.\n\n", buttons);
        player.showFormWindow(form, MAIN_FORM);
    }
    public void showCheckSignForm(Player player){
        FormWindowModal form = new FormWindowModal("§0서명", PREFIX + "정말 사인하실건가요?\n" + PREFIX + "나중에 취소할 수 없습니다.", "네", "아니요");
        player.showFormWindow(form, CHECK_SIGN_FORM);
    }

    public void showSignList(Player player){
        StringBuilder sb = new StringBuilder("§o");
        ArrayList<String> list = this.data.get(this.queue.get(player.getName()));
        for(String sign : list){
            String[] strings = sign.split("_");
            sb
                    .append("§b")
                    .append(strings[0])
                    .append("§f님의 사인 (")
                    .append(strings[1])
                    .append(")")
                    .append("\n");
        }
        FormWindowSimple form = new FormWindowSimple("§0서명목록", sb.toString());
        player.showFormWindow(form);
    }
}
